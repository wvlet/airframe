package wvlet.airframe.http.okhttp

import okhttp3.{Interceptor, Response, ResponseBody}
import wvlet.airframe.control.ResultClass
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.HttpClientMaxRetryException

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class OkHttpRetryInterceptor(retry: RetryContext) extends Interceptor {

  private def dispatch(retryContext: RetryContext, chain: Interceptor.Chain): Response = {
    val request = chain.request()

    val (resultClass, response) = Try(chain.proceed(request)) match {
      case Success(r) =>
        val res = inMemoryResponse(r)
        try {
          retryContext.resultClassifier(res) -> Some(res)
        } catch {
          case NonFatal(e) =>
            retryContext.errorClassifier(e) -> Some(res)
        }
      case Failure(e) =>
        retryContext.errorClassifier(e) -> None
    }

    resultClass match {
      case ResultClass.Succeeded =>
        response.get
      case ResultClass.Failed(isRetryable, cause, extraWait) =>
        if (!retryContext.canContinue) {
          // Reached the max retry
          throw HttpClientMaxRetryException(OkHttpResponseWrapper(response.orNull), retryContext, cause)
        } else if (!isRetryable) {
          // Non-retryable failure
          throw cause
        } else {
          // Update the retry count
          val nextRetryContext = retryContext.withExtraWait(extraWait).nextRetry(cause)
          // Wait until the next retry
          Thread.sleep(nextRetryContext.nextWaitMillis)
          // Run the same request again
          dispatch(nextRetryContext, chain)
        }
    }
  }

  private def inMemoryResponse(response: Response) = {
    Option(response.body)
      .map { body =>
        // The only stateful object is a body's source, which is a one-shot value and then must be closed.
        // Replace it with a byte array in memory so that we can pass the response along.
        val r = response
          .newBuilder().body(ResponseBody.create(body.contentType, Try(body.bytes).getOrElse(Array.empty))).build()
        body.close()
        r
      }.getOrElse {
        // Response is not eligible for a body, which means it must not be closed
        response
      }
  }

  override def intercept(chain: Interceptor.Chain): Response = {
    val request      = chain.request()
    val retryContext = retry.init(Option(request))
    dispatch(retryContext, chain)
  }
}
