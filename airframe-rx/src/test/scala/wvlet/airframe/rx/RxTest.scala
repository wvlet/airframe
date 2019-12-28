package wvlet.airframe.rx
import wvlet.airspec._

class RxTest extends AirSpec {
  def `create a new variable`: Unit = {
    val v  = Rx(1)
    val rx = v.map(x => s"count: ${x}").withName("sample rx")

    rx.subscribe(x => logger.info(x))
    v := 2
  }
}
