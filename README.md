# flex

![icon by https://thenounproject.com/ivankostriukov/](flex.png)

**WIP**

> Hold your horses!

Library that implements various methods from TCP congestion control to
limit load on a resource. It's said to be adaptive in the sense that
limits will evolve over time as we observe/measure latency.

We provide both a middleware and an interceptor that will limit
concurrency according to the limit algo specified.

A typical setup is composed of :

* a Limit: defines how limit evolve from RTT averages, dropped
  requests, timeout. Flex has both
  additive-increase/multiplicative-decrease (AIMD) and TCP Vegas
  implementations for now

* a Limiter : responsible to handle attempt of acquisition of
  permission to proceed from current limit from the Limit and the
  current in-flight requests.  The simple limiter just compares limits
  against in-flight requests, but we can have a percentage based
  implementation if needed (soon), that can be handy if you want to
  assign quotas to clients for instance.

* Recorder: records current in-flight requests numbers

* Sampler: samples latencies for a context, for instance for a sliding
  window of N requests or for a time interval.

* Clock: registers time and handle time comparaisons for the
  implementations of middlewares/interceptors. The default (sys) is a
  very naive, potentially non monotonic clock, based on
  System/nanoTime.

They are split that way so that you can compose simple but also
complex systems. You can imagine sharing a Limit with N Limiters that
would each have their own quotas, ex if you build a proxy for multiple
upstreams that should have different quotas.

From all of these components, calls to a resource will trigger a call
to the Limiter to check if we can proceed with the request or if it
should be rejected.

If the request is accepted we will record it's
[RTT](https://en.wikipedia.org/wiki/Round-trip_delay) and then send
that value to the Sampler to be able later to get an average latency
of to compute the a new Limit. Typically when we observe that latency
increases we might decrease the acceptable `limit` and in case of
latency decrease we would slowly go back up in limits. Depending on
the Limit implementation used the rate at which `limit`
increase/decrease happens can vary.

Overtime we would see the actual concurrency `limit` of a service
stabilize to a level that is its actual acceptable rate for near
optimal operation.

## Installation

[wip]

## Usage

For now just playing via https://github.com/mpenet/flex/blob/main/dev/playground.clj

## Options

[wip]

## Examples

[wip]

## License

Copyright © 2021 Max Penet

Distributed under the Eclipse Public License version 1.0.
