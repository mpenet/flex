# flex

![icon by https://thenounproject.com/ivankostriukov/](flex.png){:height="70%" width="70%"}

**WIP**

> Hold your horses!

Library that implements various methods from TCP congestion control to
request *limiting*. It's said to be adaptive in the sense that limits
will evolve over time as we observe/measure latency.

We provide both a middleware and an interceptor that will limit
concurrency according to the limit algo specified.

A typical setup is composed of :

* a Limit: defines how limit evolve from RTT averages, dropped
  requests, timeout. Flex has both
  additive-increase/multiplicative-decrease (AIMD) and TCP Vegas
  implementations for now (Vegas is an adaptation of the
  implementation in https://github.com/Netflix/concurrency-limits).

* a Limiter : responsible to handle attempt of acquisition of
  permission to proceed from current limit from the Limit and the
  current in-flight requests. The simple limiter just compares limits
  against in-flight requests, potentially with quotas per Limiter
  instance for a single Limit.

* Sampler: samples latencies for a context, for instance for a sliding
  window of N requests or for a time interval.

If the request is accepted we will record it's
[RTT](https://en.wikipedia.org/wiki/Round-trip_delay) and then send
that value to the Sampler to be able later to get an average latency
of to compute the a new Limit. Typically when we observe that latency
increases we might decrease the acceptable `limit` and in case of
latency decrease we would slowly go back up in limits. Depending on
the Limit implementation used the rate at which `limit`
increase/decrease happens can vary. Then the middleware or the
interceptor or whatever you choose to implement on top, can check the
number of current in-flight requests against that limit to decide to
reject/accept/ignore it.

Overtime we would see the actual concurrency `limit` of a service
stabilize to a level that is its actual acceptable rate for near
optimal operation.

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
