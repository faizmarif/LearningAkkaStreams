import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

public class SimpleStream {

  public static void main(String[] args) {
    //Source<Integer, NotUsed> source = Source.range(1, 10, 2); // source of range of elements
    //Source<Integer, NotUsed> source = Source.single(15); // source of a single element

    List<String> names = List.of("qwer", "poiu", "asdf");
    Source<String, NotUsed> source = Source.from(names); // source from a list

    Source<Double, NotUsed> piSource = Source.repeat(3.141592654);
    Source<String, NotUsed> repeatingNamesSource = Source.cycle(names::iterator);

    Iterator<Integer> infiniteRange = Stream.iterate(0, i -> i+1).iterator(); // this line of code is pure java. It has nothing to with akka.
    Source<Integer, NotUsed> infiniteRangeSource = Source.
        fromIterator(() -> infiniteRange)
        .throttle(1, Duration.ofSeconds(2)) // send maximum of 1 element in 2 seconds
        .take(5); // send only first 5 elements and then stop the source

    Flow<Integer, String, NotUsed> flow = Flow.of(Integer.class).map(value -> "The next value is " + value);
    Flow<String, String, NotUsed> stringFlow = Flow.of(String.class).map(value -> "The next value is " + value);
    Flow<Double, String, NotUsed> doubleFlow = Flow.of(Double.class).map(value -> "The next value is " + value);

    Sink<String, CompletionStage<Done>> sink = Sink.foreach(System.out::println);
    Sink<String, CompletionStage<Done>> ignoreSink = Sink.ignore(); // ignores the values received

    RunnableGraph<NotUsed> graph = infiniteRangeSource.via(flow).to(sink);

    ActorSystem actorSystem = ActorSystem.create(Behaviors.empty(), "actor-system");
    graph.run(actorSystem);


    // other ways to write stream
    sink.runWith(repeatingNamesSource.via(stringFlow), actorSystem);
    // equivalent to
    repeatingNamesSource.via(stringFlow).to(sink).run(actorSystem); // we will use this one
    // equivalent to
    repeatingNamesSource.via(stringFlow).runForeach(System.out::println, actorSystem);
  }

}
