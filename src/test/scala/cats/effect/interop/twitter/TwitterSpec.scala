package cats.effect.interop.twitter

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}
import cats.effect.interop.twitter.syntax._
import org.specs2.mutable.Specification
import cats.implicits._
import cats.effect._
import cats.effect.internals.IOAppPlatform
import com.twitter.util.{Await, Future, JavaTimer, TimeoutException}
import com.twitter.conversions.DurationOps._

class TwitterSpec extends Specification {
  implicit val timer: JavaTimer               = new JavaTimer(true)
  implicit val contextShift: ContextShift[IO] = IOAppPlatform.defaultContextShift
  implicit val ioTimer: Timer[IO]             = IOAppPlatform.defaultTimer

  "fromFuture should" >> {

    "work for delayed value" >> {
      val f = IO(Future.sleep(300.millis).map(_ => 5))
      f.fromFuture.unsafeRunSync() must_== 5
    }

    "work for for finished future" >> {
      val f = IO.pure(Future.value(10))
      f.fromFuture.unsafeRunSync() must_== 10
    }

    "execute side-effects" >> {
      val c = new AtomicInteger(0)

      val f = IO(Future.sleep(100.millis).map(_ => c.incrementAndGet()))
      List.fill(10)(f).traverse(_.fromFuture).unsafeRunSync() must_== (1 to 10).toList
      c.get must_== 10
    }

    "cancel the underlying future " >> {
      val c = new AtomicInteger(0)

      val fa = IO(Future.sleep(300.millis).map(_ => c.incrementAndGet())).fromFuture
      val fb = IO.sleep(FiniteDuration(100, MILLISECONDS)) >> IO("OK")

      IO.race(fa, fb).unsafeRunSync() must beRight("OK")
      MILLISECONDS.sleep(500L)
      c.get must_== 0
    }

  }

  "unsafeRunAsyncT should" >> {

    "execute sync IO[A]" >> {
      Await.result(unsafeRunAsyncT(IO(1))) must_== 1
    }

    "execute async IO[A]" >> {
      Await.result(unsafeRunAsyncT(IO.sleep(FiniteDuration(100, MILLISECONDS)).map(_ => 1))) must_== 1
    }

    "cancel IO" >> {
      val c = new AtomicInteger(0)

      val f = IO.sleep(FiniteDuration(300, MILLISECONDS)) >> IO(c.incrementAndGet())

      val t = f.unsafeRunAsyncT
      Await.result(t, 100.millis) must throwA[TimeoutException]
      t.raise(new TimeoutException("timeout"))

      MILLISECONDS.sleep(500L)
      c.get() should_== 0
    }

  }

}
