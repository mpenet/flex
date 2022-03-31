# flex

<img src="flex.png" width="133" height="133" alt="icon by https://thenounproject.com/ivankostriukov/"/>

**WIP**

> Hold your horses!

Library that implements various methods from TCP congestion control to request
*limiting*. It's said to be adaptive in the sense that the `concurrency limit`
of a system will evolve over time as we observe/measure average request
round trip latency.

We provide both a middleware and an interceptor that will limit
concurrency according to the limit algo specified.

A typical setup is composed of :

* a *Limit*: defines how the `concurrency limit` of a context evolve from
  [RTT](https://en.wikipedia.org/wiki/Round-trip_delay) averages, dropped
  requests, timeout. Flex has an additive-increase/multiplicative-decrease
  (AIMD) implementation right now and a few others more experimental.

* *Sampler*: samples RTT for a context in order to compute an average to be used
later by `Limit` when computing a new `concurrency limit`. It's pluggable so you
can imagine more fine grained sampling, time-boxed, multi-values (min/max),
percentiles and whatnot. It's really easy to implement/extend.

* *Request* : takes a `Sampler` and a `Limit`implementation and allows to
performs recording of a request Lifecycle (accepted/rejected/completed) and
trigger subsequent `concurrency limits` update. Typically upon every hit to a
system a Request is *acquired* for checks/update.

* *Limiter* (naming subject to change) : very thin component that will
  encapsulate a sampler/limit/request implementation and allow to `acquire` a
  *Request* to inspect/update the `concurrency limits` depending on the request
  lifecycle (accept/reject/etc). We can also assign quotas at this level: we can
  have many *Limiters* per *Limit*/*Sampler*, this allows to say client A should
  only be allowed 20% of the available concurrency limits against that system
  and so on (it's set per Limiter instance). 

How it works in practice:

Typically when we observe that average latency increase we might decrease the
acceptable `concurrency limit` value and in case of average latency decrease we
would slowly increase that value. Depending on the *Limit* implementation used,
the rate at which the `concurrency limit` increase/decrease happens can vary.
Then the middleware or the interceptor or whatever you choose to implement on
top, would compare the number of current in-flight requests against the current
`concurrency limit` to decide to reject/accept/ignore it and then trigger an
update of the `concurrency limit` accordingly.

Over time we would see the actual `concurrency limit` of a service
stabilise/converge to a level that is its actual acceptable rate for near
optimal operation and it would adapt with the health of the system it protects:
if the system is stable it will try to increase limits slowly up to `max-limit`,
if it's struggling it will lower the limit and cause requests to be rejected at
the edge.

This strategies can be applied to many context, they can be used at
client level, server, queues, executors, per API endpoint, etc...


## Installation

{:deps {com.s-exp/flex {:git/sha "..." :git/url "https://github.com/mpenet/flex"}}}

## Usage

For now just playing via https://github.com/mpenet/flex/blob/main/dev/playground.clj

The [ring middleware](https://github.com/mpenet/flex/blob/main/src/s-exp/flex/middleware.clj) is also a very simple example of how things work on the surface:

## Options

[wip]

## Examples

[wip]

## License

Copyright © 2022 Max Penet

Distributed under the Eclipse Public License version 1.0.
