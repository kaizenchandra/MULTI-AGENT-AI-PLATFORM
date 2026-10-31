package com.kaizen.platform;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;
class BoundedResponseTest {
 static class Subscription implements Flow.Subscription {boolean cancelled;public void request(long n){}public void cancel(){cancelled=true;}}
 @Test void oversizedResponseCancelledBeforeAllocation(){var b=new BoundedResponse(10);var s=new Subscription();b.onSubscribe(s);b.onNext(List.of(ByteBuffer.wrap(new byte[11])));assertTrue(s.cancelled);assertThrows(CompletionException.class,()->b.getBody().toCompletableFuture().join());}
 @Test void boundedResponseAccumulatesChunks(){var b=new BoundedResponse(10);b.onSubscribe(new Subscription());b.onNext(List.of(ByteBuffer.wrap(new byte[]{1,2}),ByteBuffer.wrap(new byte[]{3})));b.onComplete();assertArrayEquals(new byte[]{1,2,3},b.getBody().toCompletableFuture().join());}
}
