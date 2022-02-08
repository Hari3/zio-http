package zhttp.service

import io.netty.handler.codec.http.{DefaultFullHttpRequest, FullHttpRequest, HttpHeaderNames}
import zio.Task

trait EncodeClientRequest {

  /**
   * Converts client params to JFullHttpRequest
   */
  def encode(req: Client.ClientRequest): Task[FullHttpRequest] =
    req.getBodyAsByteBuf.map { content =>
      val method   = req.method.toJava
      val jVersion = req.version.toJava

      // As per the spec, the path should contain only the relative path.
      // Host and port information should be in the headers.
      val path = req.url.getRelative.encode

      val encodedReqHeaders = req.headers.encode

      val headers = req.url.getHost match {
        case Some(value) if value != null => encodedReqHeaders.set(HttpHeaderNames.HOST, value)
        case _        => encodedReqHeaders
      }

      val writerIndex = content.writerIndex()
      if (writerIndex != 0) {
        headers.set(HttpHeaderNames.CONTENT_LENGTH, writerIndex.toString)
      }

      // TODO: we should also add a default user-agent req header as some APIs might reject requests without it.
      val jReq = new DefaultFullHttpRequest(jVersion, method, path, content)
      jReq.headers().set(headers)

      jReq
    }
}
