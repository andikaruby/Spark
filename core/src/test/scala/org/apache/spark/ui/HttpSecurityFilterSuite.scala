/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.ui

import java.util.UUID
import javax.servlet.FilterChain
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import scala.collection.JavaConverters._

import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.{mock, never, times, verify, when}

import org.apache.spark._
import org.apache.spark.internal.config._

class HttpSecurityFilterSuite extends SparkFunSuite {

  test("disallow bad user input") {
    val badValues = Map(
      "encoded" -> "Encoding:base64%0d%0a%0d%0aPGh0bWw%2bjcmlwdD48L2h0bWw%2b",
      "alert1" -> """>"'><script>alert(401)<%2Fscript>""",
      "alert2" -> """app-20161208133404-0002<iframe+src%3Djavascript%3Aalert(1705)>""",
      "alert3" -> """stdout'%2Balert(60)%2B'""",
      "html" -> """stdout'"><iframe+id%3D1131+src%3Dhttp%3A%2F%2Fdemo.test.net%2Fphishing.html>"""
    )
    val badKeys = badValues.map(_.swap)
    val badInput = badValues ++ badKeys
    val goodInput = Map("goodKey" -> "goodValue")

    val conf = new SparkConf()
    val filter = new HttpSecurityFilter(conf, new SecurityManager(conf))

    def newRequest(): HttpServletRequest = {
      val req = mock(classOf[HttpServletRequest])
      when(req.getParameterMap()).thenReturn(Map.empty[String, Array[String]].asJava)
      req
    }

    def verifyError(req: HttpServletRequest): Unit = {
      val chain = mock(classOf[FilterChain])
      val res = mock(classOf[HttpServletResponse])
      filter.doFilter(req, res, chain)
      verify(chain, never()).doFilter(any(), any())
      verify(res).sendError(meq(HttpServletResponse.SC_BAD_REQUEST), any())
    }

    def verifySuccess(req: HttpServletRequest): Unit = {
      val chain = mock(classOf[FilterChain])
      val res = mock(classOf[HttpServletResponse])
      filter.doFilter(req, res, chain)
      verify(chain).doFilter(any(), any())
    }

    badInput.foreach { case (k, v) =>
      val req = newRequest()
      when(req.getParameterMap()).thenReturn(Map(k -> Array(v)).asJava)
      verifyError(req)
    }

    goodInput.foreach { case (k, v) =>
      val req = newRequest()
      when(req.getParameterMap()).thenReturn(Map(k -> Array(v)).asJava)
      verifySuccess(req)
    }
  }

  test("perform access control") {
    val conf = new SparkConf(false)
      .set("spark.ui.acls.enable", "true")
      .set("spark.admin.acls", "admin")
      .set("spark.ui.view.acls", "alice")
    val secMgr = new SecurityManager(conf)

    val req = mockEmptyRequest()
    val res = mock(classOf[HttpServletResponse])
    val chain = mock(classOf[FilterChain])

    val filter = new HttpSecurityFilter(conf, secMgr)

    when(req.getRemoteUser()).thenReturn("admin")
    filter.doFilter(req, res, chain)
    verify(chain, times(1)).doFilter(any(), any())

    when(req.getRemoteUser()).thenReturn("alice")
    filter.doFilter(req, res, chain)
    verify(chain, times(2)).doFilter(any(), any())

    // Because the current user is added to the view ACLs, let's try to create an invalid
    // name, to avoid matching some common user name.
    when(req.getRemoteUser()).thenReturn(UUID.randomUUID().toString())
    filter.doFilter(req, res, chain)

    // chain.doFilter() should not be called again, so same count as above.
    verify(chain, times(2)).doFilter(any(), any())
    verify(res).sendError(meq(HttpServletResponse.SC_FORBIDDEN), any())
  }

  test("set security-related headers") {
    val conf = new SparkConf(false)
      .set("spark.ui.allowFramingFrom", "example.com")
      .set(UI_X_XSS_PROTECTION, "xssProtection")
      .set(UI_X_CONTENT_TYPE_OPTIONS, true)
      .set(UI_STRICT_TRANSPORT_SECURITY, "tsec")
    val secMgr = new SecurityManager(conf)
    val req = mockEmptyRequest()
    val res = mock(classOf[HttpServletResponse])
    val chain = mock(classOf[FilterChain])

    when(req.getScheme()).thenReturn("https")

    val filter = new HttpSecurityFilter(conf, secMgr)
    filter.doFilter(req, res, chain)

    Map(
      "X-Frame-Options" -> "ALLOW-FROM example.com",
      "X-XSS-Protection" -> "xssProtection",
      "X-Content-Type-Options" -> "nosniff",
      "Strict-Transport-Security" -> "tsec"
    ).foreach { case (name, value) =>
      verify(res).setHeader(meq(name), meq(value))
    }
  }

  private def mockEmptyRequest(): HttpServletRequest = {
    val params: Map[String, Array[String]] = Map.empty
    val req = mock(classOf[HttpServletRequest])
    when(req.getParameterMap()).thenReturn(params.asJava)
    req
  }

}
