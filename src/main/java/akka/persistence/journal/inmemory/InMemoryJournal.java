package akka.persistence.journal.inmemory;

import akka.dispatch.Futures;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import akka.persistence.Deliver;
import akka.persistence.PersistentConfirmation;
import akka.persistence.PersistentId;
import akka.persistence.PersistentRepr;
import akka.persistence.journal.japi.SyncWriteJournal;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import scala.concurrent.Future;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.Callable;

import static scala.collection.JavaConversions.asScalaBuffer;

public class InMemoryJournal extends SyncWriteJournal {

  private LoggingAdapter log = Logging.getLogger( context().system(), this );
  private Map<String, Map<Long, PersistentRepr>> persistentMap = Maps.newHashMap();

  @Override
  public Future<Void> doAsyncReplayMessages( final String processorId, final long fromSequenceNr,
                                             final long toSequenceNr, final long max,
                                             final Procedure<PersistentRepr> replayCallback ) {

    return Futures.future( new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        Map<Long, PersistentRepr> replayMap = Maps.filterValues( persistentMap( processorId ),
                                                                 new Predicate<PersistentRepr>() {
                                                                   @Override
                                                                   public boolean apply( PersistentRepr message ) {
                                                                     if ( message.sequenceNr() >= fromSequenceNr &&
                                                                          message.sequenceNr() <= toSequenceNr ) {
                                                                       return true;
                                                                     }
                                                                     return false;
                                                                   }
                                                                 } );
        int count = 0;
        for ( PersistentRepr message : replayMap.values() ) {
          if ( count++ < max ) {
            replayCallback.apply( message );
          }
        }
        return null;
      }
    }, context().dispatcher() );
  }

  @Override
  public Future<Long> doAsyncReadHighestSequenceNr( final String processorId, long fromSequenceNr ) {
    return Futures.future( new Callable<Long>() {
      @Override
      public Long call() throws Exception {
        TreeMap<Long, PersistentRepr> map = (TreeMap<Long,
            PersistentRepr>) persistentMap( processorId );
        if ( map.isEmpty() ) {
          return 0L;
        }
        return map.lastKey();
      }
    }, context().dispatcher() );
  }

  @Override
  public void doWriteMessages( Iterable<PersistentRepr> messages ) {
    for ( PersistentRepr persistent : messages ) {
      persistentMap( persistent.processorId() ).put( persistent.sequenceNr(), persistent );

    }
  }

  @Override
  public void doWriteConfirmations( Iterable<PersistentConfirmation> confirmations ) {
    for ( PersistentConfirmation confirmation : confirmations ) {
      Map<Long, PersistentRepr> map = persistentMap( confirmation.processorId() );
      PersistentRepr persistentRepr = map.get( confirmation.sequenceNr() );
      if ( persistentRepr != null ) {
        ArrayList<String> strings = Lists.newArrayList( persistentRepr.getConfirms() );
        strings.add( confirmation.channelId() );
        PersistentRepr update = persistentRepr.update( confirmation.sequenceNr(),
                                                       confirmation.processorId(),
                                                       persistentRepr.deleted(),
                                                       persistentRepr.redeliveries(),
                                                       asScalaBuffer( strings ).toList(),
                                                       persistentRepr.confirmMessage(),
                                                       persistentRepr.confirmTarget(),
                                                       persistentRepr.sender() );
        map.put( update.sequenceNr(), update );
      }

    }
  }

  @Override
  public void doDeleteMessages( Iterable<PersistentId> messageIds, boolean permanent ) {
    for ( PersistentId id : messageIds ) {
      if ( permanent ) {
        removeItem( id.processorId(), id.sequenceNr() );
      } else {
        Map<Long, PersistentRepr> map = persistentMap( id.processorId() );
        PersistentRepr persistentRepr = map.get( id.sequenceNr() );
        if ( persistentRepr != null ) {
          PersistentRepr update = persistentRepr.update( id.sequenceNr(), id.processorId(), true,
                                                         persistentRepr.redeliveries(),
                                                         persistentRepr.confirms(),
                                                         persistentRepr.confirmMessage(),
                                                         persistentRepr.confirmTarget(),
                                                         persistentRepr.sender() );
          map.put( update.sequenceNr(), update );
        }
      }
    }
  }

  private void removeItem( String processorId, long sequenceNr ) {
    persistentMap( processorId ).remove( sequenceNr );
    if ( persistentMap( processorId ).isEmpty() ) {
      persistentMap.remove( processorId );
    }
  }

  private Map<Long, PersistentRepr> persistentMap( String processorId ) {
    Map<Long, PersistentRepr> map = persistentMap.get( processorId );
    if ( map == null ) {
      map = Maps.newTreeMap();
      persistentMap.put( processorId, map );
    }
    return map;
  }

  @Override
  public void doDeleteMessagesTo( String processorId, final long toSequenceNr, boolean permanent ) {
    Map<Long, PersistentRepr> reprMap;
    if ( permanent ) {
      reprMap = Maps.filterKeys( persistentMap(
          processorId ), new Predicate<Long>() {
        @Override
        public boolean apply( Long input ) {
          return input > toSequenceNr;
        }
      } );
    } else {
      reprMap = Maps.transformValues( persistentMap( processorId ), new Function<PersistentRepr, PersistentRepr>() {
        @Nullable
        @Override
        public PersistentRepr apply( @Nullable PersistentRepr persistentRepr ) {
          if ( persistentRepr.sequenceNr() <= toSequenceNr ) {
            return persistentRepr.update( persistentRepr.sequenceNr(), persistentRepr.processorId(), true,
                                          persistentRepr.redeliveries(),
                                          persistentRepr.confirms(),
                                          persistentRepr.confirmMessage(),
                                          persistentRepr.confirmTarget(),
                                          persistentRepr.sender() );
          }
          return persistentRepr;
        }
      } );
    }
    HashMap<Long, PersistentRepr> newMap = new HashMap<>( reprMap );
    persistentMap.put( processorId, newMap );
  }

  @Override
  public void postStop() throws Exception {
    if ( !isEmpty() ) {
      log.error( "\n--------------------------\n" +
                 "Journal is dirty\n{}" +
                 "--------------------------",
                 Joiner.on( '\n' ).join( mapToString() ) + "\n" );
    }
    persistentMap.clear();
  }

  private boolean isEmpty() {
    boolean result = true;
    for ( Map<Long, PersistentRepr> map : persistentMap.values() ) {
      for ( PersistentRepr value : map.values() ) {
        result &= value.deleted() || !value.getConfirms().isEmpty();
      }
    }
    return result;
  }

  private Collection<String> mapToString() {
    List<String> result = Lists.newArrayList();
    for ( Map.Entry<String, Map<Long, PersistentRepr>> entry : persistentMap.entrySet() ) {
      result.add( "processor: " + entry.getKey() );
      Map<Long, PersistentRepr> reprMap = entry.getValue();
      for ( PersistentRepr value : reprMap.values() ) {
        if ( !value.deleted() && value.getConfirms().isEmpty() ) {
          if ( value.payload() instanceof Deliver ) {
            result.add( "payload: " + ((Deliver) value.payload()).persistent().payload() );
          } else {
            result.add( "message: " + value );
          }
        }
      }
      result.add( "\n" );
    }
    return result;
  }
}
