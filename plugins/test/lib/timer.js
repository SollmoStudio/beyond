/* global JavaAdapter */

var executor = new java.util.concurrent.Executors.newScheduledThreadPool(1);
var counter = 1;
var ids = {};

exports.mount = function (global) {
  var setTimeout = function (fn,delay) {
    var id = counter++;
    var runnable = new JavaAdapter(java.lang.Runnable, {run: fn});
    ids[id] = executor.schedule(runnable, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    return id;
  };

  var clearTimeout = function (id) {
    ids[id].cancel(true);
    executor.purge();
    delete ids[id];
  };

  var setInterval = function (fn,delay) {
    var id = counter++;
    var runnable = new JavaAdapter(java.lang.Runnable, {run: fn});
    ids[id] = executor.scheduleAtFixedRate(runnable, delay, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    return id;
  };

  var clearInterval = clearTimeout;

  global.setTimeout = setTimeout;
  global.clearTimeout = clearTimeout;
  global.setInterval = setInterval;
  global.clearInterval = clearInterval;
};
