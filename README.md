# flex

**WIP**

> Hold your horses!

Library that implements various methods from TCP congestion control to
limit load on a resource. It's said to be adaptive in the sense that
limits will evolve over time as we observe/measure latency.


We provide both a middleware and an interceptor that will limit
concurrency according to the limit algo specified.

A middleware is composed of :

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


## Installation

Download from https://github.com/mpenet/flex

## Usage
## Options
## Examples

## License

Copyright © 2021 Max Penet

Distributed under the Eclipse Public License version 1.0.
