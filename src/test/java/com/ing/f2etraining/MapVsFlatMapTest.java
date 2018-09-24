package com.ing.f2etraining;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import akka.util.Timeout;
import com.ing.f2etraining.model.Person;
import errors.impl.MyError;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;
import util.Java8;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import errors.GenericError;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

public class MapVsFlatMapTest {

    private static final ExecutionContext EXECUTOR = ExecutionContexts.fromExecutor(Executors.newSingleThreadExecutor());
    private static final Timeout TIMEOUT = new Timeout(Duration.create(5, "seconds"));

    @Test
    public void useMapOnList() {
        //given
        List<Person> people = Arrays.asList(
                new Person().setName("Juan").setAge(35),
                new Person().setName("Miguel").setAge(34),
                new Person().setName("David").setAge(28)
        );

        //when
        //Hint: You have to use map
        List<String> names = people.stream()
                                .map( person -> person.getName() )
                                .collect(Collectors.toList());

        //then
        assertThat(names).containsOnly("Juan", "David", "Miguel");
    }

    @Test
    public void useMapOnOptional() {
        //given
        Optional<Person> personOptional = Optional.of(new Person().setName("Juan").setAge(35));

        //when
        //Hint: You have to use map
        Optional<String> nameOptional = personOptional.map( person -> person.getName() );

        //then
        assertThat(nameOptional).contains("Juan");
    }

    @Test
    public void useMaoOnFutures() throws Exception {
        //given
        Future<Person> personFuture = Futures.successful(new Person().setName("Juan").setAge(35));

        //when
        //Hint: You have to use map
        Future<String> nameF = personFuture.map(person -> person.getName(), EXECUTOR);

        //then
        String name = (String) Await.result(nameF, TIMEOUT.duration());
        assertThat(name).isEqualTo("Juan");
    }

    @Test
    public void useFlatMapOnList() {
        //given
        List<Person> people = Arrays.asList(
                new Person().setName("Juan").setAge(35),
                new Person().setName("Miguel").setAge(34),
                new Person().setName("David").setAge(28)
        );

        //when
        //Hint: You have to use flatMap
        List<String> names = people.stream()
                                .flatMap( person -> Stream.of(person.getName()) )
                                .collect(Collectors.toList());

        //then
        assertThat(names).containsOnly("Juan", "David", "Miguel");
    }

    @Test
    public void useFlatMapOnOptional() {
        //given
        Optional<Person> personOptional = Optional.of(new Person().setName("Juan").setAge(35));

        //when
        //Hint: You have to use flatMap
        Optional<String> nameOptional = personOptional.flatMap( person -> Optional.ofNullable(person.getName()) );

        //then
        assertThat(nameOptional).contains("Juan");
    }

    @Test
    public void useFlatMapOnFuture() throws Exception {
        //given
        Future<Person> personFuture = Futures.successful(new Person().setName("Juan").setAge(35));

        //when
        //Hint: You have to use flatMap
        Future<String> nameF = personFuture.flatMap(person -> Futures.successful(person.getName()), EXECUTOR);

        //then
        String name = (String) Await.result(nameF, TIMEOUT.duration());
        assertThat(name).isEqualTo("Juan");
    }
}
