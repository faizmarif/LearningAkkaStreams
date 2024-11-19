import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.stream.Attributes;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class BigPrimes {

  public static void main(String[] args) {
    Long start = System.currentTimeMillis();
    ActorSystem actorSystem = ActorSystem.create(Behaviors.empty(), "actorSystem");

    Source<Integer, NotUsed> source = Source.range(1, 10);

    Flow<Integer, BigInteger, NotUsed> bigIntegerFlow = Flow.of(Integer.class)
        .map(v -> {
          BigInteger bigInteger = new BigInteger(3000, new Random());
          System.out.println("BigInteger: " + bigInteger);
          return bigInteger;
        });
    Flow<BigInteger, BigInteger, NotUsed> bigPrimeFlow = Flow.of(BigInteger.class)
        .map(v -> {
          BigInteger prime = v.nextProbablePrime();
          System.out.println("Prime:" + prime);
          return prime;
        });

    Flow<BigInteger, BigInteger, NotUsed> bigPrimeFlowAsync = Flow.of(BigInteger.class)
        .mapAsyncUnordered(8, v -> {
          CompletableFuture<BigInteger> futurePrime = new CompletableFuture<>();
          futurePrime.completeAsync(() -> {
            BigInteger prime = v.nextProbablePrime();
            System.out.println("Prime:" + prime);
            return prime;
          });
          return futurePrime;
        });

    Flow<BigInteger, List<BigInteger>, NotUsed> createGroup = Flow.of(BigInteger.class)
        .grouped(10)
        .map(v -> {
          List<BigInteger> list = new ArrayList<>(v);
          Collections.sort(list);
          return list;
        });

    Sink<List<BigInteger>, CompletionStage<Done>> printSink = Sink.foreach(System.out::println);

    CompletionStage<Done> result = source
        .via(bigIntegerFlow)
        //.buffer(16, OverflowStrategy.backpressure())
        .async()
        .via(bigPrimeFlowAsync)
        .async()
        .via(createGroup)
        .toMat(printSink, Keep.right())
        .run(actorSystem);

    result.whenComplete((r, t) -> {
      Long end = System.currentTimeMillis();
      System.out.println("The application ran in " + (end-start) + "ms.");
      actorSystem.terminate();
    });
  }

}
