# flex

<img src="flex.png" width="133" height="133" alt="icon by https://thenounproject.com/ivankostriukov/"/>

**WIP**

> Hold your horses!

Library that implements various methods from TCP congestion control to
request *limiting*. It's said to be adaptive in the sense that limits
will evolve over time as we observe/measure average request latency.

We provide both a middleware and an interceptor that will limit
concurrency according to the limit algo specified.

A typical setup is composed of :

* a Limit: defines how limit evolve from RTT averages, dropped
  requests, timeout. Flex has both
  additive-increase/multiplicative-decrease (AIMD), TCP Vegas
  implementations for now (Vegas is an adaptation of the
  implementation in https://github.com/Netflix/concurrency-limits) and
  soon a Gradient based implementation.

* a Limiter : responsible to handle attempt of acquisition of
  permission to proceed from current `Limit` and the current in-flight
  requests at a time. The default limiter just compares `Limit`
  against in-flight requests, potentially with quotas per Limiter
  instance for a single Limit.

* Sampler: the initial impelmentation samples latencies for a context,
  for instance for a sliding window of N requests or for a time
  interval, but it's pluggable to you can imagine more fine grained
  sampling, time-boxed, multi-values (min/max), percentiles and
  whatnot. It's really easy to implement/extend.


How it works:

When a request comes in we will check if it's accepted against the
current limit+sampling. Typically a Limiter is initialized with a
"desired" limit, max-limit and min-limit, there are also other values
depending on the algorithm selected that will impact
increase/decreases/sampling.

If the request is accepted we will record it's
[RTT](https://en.wikipedia.org/wiki/Round-trip_delay) to the the
Sampler to be able later to get an average latency value, to be able
to compute a new `Limit` for the whole system.

Typically when we observe that average latency increase we might
decrease the acceptable `Limit` and in case of latency decrease we
would slowly go increase the `Limit`. Depending on the `Limit`
implementation used the rate at which `Limit` increase/decrease
happens can vary.  Then the middleware or the interceptor or whatever
you choose to implement on top, can check the number of current
in-flight requests against that limit to decide to
reject/accept/ignore it.

Overtime we would see the actual concurrency limit of a service
stabilize to a level that is its actual acceptable rate for near
optimal operation and it would adapt with the health of the system it
protects: if the system is stable it will try to increase limits
slowly up to `max-limit`, if it's struggling it will lower the limit
and cause requests to be rejected at the edge.

This strategies can be applied to many context, they can be used at
client level, server, queues, executors etc.

## Installation

[wip]

## Usage

For now just playing via https://github.com/mpenet/flex/blob/main/dev/playground.clj

The [ring middleware](https://github.com/mpenet/flex/blob/main/src/qbits/flex/middleware.clj) is also a very simple example of how things work on the surface:

## Options

[wip]

## Examples

[wip]

## License

Copyright © 2021 Max Penet

Distributed under the Eclipse Public License version 1.0.
