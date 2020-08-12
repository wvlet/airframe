package wvlet.airframe.http.rx

trait RxStream[+A] extends Rx[A] {}

object RxStream {
  def fromSeq[A](values: Seq[A]): RxStream[A] = ???
}
