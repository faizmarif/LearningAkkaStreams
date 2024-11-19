import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class ExploringFlows {

  public static void main(String[] args) {
    ActorSystem actorSystem = ActorSystem.create(Behaviors.empty(), "actorSystem");

    Source<Integer, NotUsed> numbers = Source.range(1, 200);

    Flow<Integer, Integer, NotUsed> filterFlow = Flow.of(Integer.class).filter(v -> v % 17 == 0);
    Flow<Integer, Integer, NotUsed> concatFlow = Flow.of(Integer.class).mapConcat(v -> List.of(v, v+1, v+2));
    //mapConcat flattens the collections returned by the lambda function into a single stream of elements.
    Flow<Integer, Integer, NotUsed> groupFlow = Flow.of(Integer.class)
        .grouped(3)
        .map(value -> {
          List<Integer> newList = new ArrayList<>(value);
          Collections.sort(newList, Collections.reverseOrder());
          return newList;
        })
        .mapConcat(value -> value);

    Sink<Integer, CompletionStage<Done>> printSink = Sink.foreach(System.out::println);

    numbers.via(filterFlow).via(concatFlow).via(groupFlow).to(printSink).run(actorSystem);

    // chaining source to flow creates a new source
    // chaining flow to another flow creates a new flow
    // chaining flow to sink creates a new sink
  }

}
