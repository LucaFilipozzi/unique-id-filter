// Copyright (C) 2021 Luca Filipozzi. All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// request wrapper based on https://stackoverflow.com/a/23590606/507056
// generates ECS fields per https://github.com/elastic/ecs/issues/998#issuecomment-705270230

package com.github.lucafilipozzi.catalina.filters;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@SuppressWarnings("unused")
public class UniqueIdFilter implements javax.servlet.Filter {

  @Override
  public void init(FilterConfig filterConfig) {
    // intentionally empty
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    filterChain.doFilter(new UniqueIdRequest((HttpServletRequest) servletRequest), servletResponse);
  }

  @Override
  public void destroy() {
    // intentionally empty
  }

  static class UniqueIdRequest extends HttpServletRequestWrapper {
    static final String name = "x-request-id";
    String value;

    public UniqueIdRequest(HttpServletRequest request) {
      super(request);

      // extract and log trace.id, parent.id, and transaction.id
      final String[] tracingValues = request.getParameter(name).split("\\.", 2);
      // TODO add error handling
      final String traceId = tracingValues[0];
      final String parentId = tracingValues[1];
      final String transactionId = UUID.randomUUID().toString();
      request.setAttribute("x-ecs-trace-id", traceId);
      request.setAttribute("x-ecs-parent-id", parentId);
      request.setAttribute("x-ecs-transaction-id", transactionId);

      // propagate trace.id and span.id
      final String spanId = UUID.randomUUID().toString();
      request.setAttribute("x-ecs-span-id", spanId);
      value = traceId + "." + spanId;
    }

    @Override
    public String getHeader(String name) {
      if (UniqueIdRequest.name.equalsIgnoreCase(name)) {
        return value;
      }
      return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      if (UniqueIdRequest.name.equalsIgnoreCase(name)) {
        List<String> values = Collections.singletonList(value);
        return Collections.enumeration(values);
      }
      return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      List<String> names = Collections.list(super.getHeaderNames());
      names.add(UniqueIdRequest.name);
      return Collections.enumeration(names);
    }
  }
}
