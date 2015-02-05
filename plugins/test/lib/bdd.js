var reporter = require('reporter');
require('timer').mount(this);

function Bdd() {
  var bdd = this;

  function Task(name, test) {
    this.name = name;
    this.only = false;
    this.async = false;
    this.timeout = 10000; // for async, 10sec

    this.test = test;
  }

  function TaskGroup(name, depth) {
    this.name = name;
    this.only = false;
    this.depth = depth;

    this.taskGroups = [];
    this.tasks = [];
    this.after = this.before = this.afterEach = this.beforeEach = function () {};
  }

  TaskGroup.prototype.try = function (func) {
    try {
      func();
    } catch (err) {
      reporter.taskFailed(this.depth, this.name, err.name, err.message, err.stack);
      reporter.rootTaskFinished(1);
    }
  };

  TaskGroup.prototype.run = function (done) {
    var that = this;

    reporter.taskGroupStart(that.depth, that.name);

    that.try(that.before);

    var result = {successCount: 0, failureCount: 0};
    that.runTasks(result, function (result) {
      that.runTaskGroups(result, function (result) {
        that.try(that.after);
        reporter.taskGroupFinished(that.depth, that.name, result.successCount, result.failureCount);
        done(result);
      });
    });
  };

  TaskGroup.prototype.runTasks = function (result, done) {
    var that = this;
    var index = -1; // to ensure runNext starts with index 0 in the first time

    var run, runNext;

    runNext = function () {
      index++;
      setTimeout(run, 0);
    };

    run = function () {
      var task = that.tasks[index];

      if (typeof task === 'undefined') {
        done(result);
        return;
      }

      that.try(that.beforeEach);
      if (task.async) {
        (function () {
          var timeoutTimer;
          var done = false;
          function taskDone(err) {
            if (!done) {
              clearTimeout(timeoutTimer);
              if (typeof err === 'undefined') {
                result.successCount++;
                reporter.taskFinished(that.depth, task.name);
              } else {
                result.failureCount++;
                reporter.taskFailed(that.depth, task.name, err.name, err.message, err.stack);
              }
              that.try(that.afterEach);
              done = true;
              runNext();
            }
          }
          function timeout() {
            // Get stack for Rhino
            var stack;
            try { throw new Error(); } catch (e) { stack = e.stack; }
            taskDone({name: 'Timeout', message: 'Task timeout.', stack: stack});
          }
          timeoutTimer = setTimeout(timeout, task.timeout);
          task.test(taskDone);
        })();
      } else {
        try {
          task.test();
          result.successCount++;
          reporter.taskFinished(that.depth, task.name);
        } catch (err) {
          result.failureCount++;
          reporter.taskFailed(that.depth, task.name, err.name, err.message, err.stack);
        }
        that.try(that.afterEach);
        runNext();
      }
    };

    runNext();
  };

  TaskGroup.prototype.runTaskGroups = function (result, done) {
    var that = this;
    var index = -1; // to ensure runNext starts with index 0 in the first time

    var run, runNext;

    runNext = function () {
      index++;
      setTimeout(run, 0);
    };

    run = function () {
      var taskGroup = that.taskGroups[index];

      if (typeof taskGroup === 'undefined') {
        done(result);
        return;
      }

      that.try(that.beforeEach);
      taskGroup.run(function (taskGroupResult) {
        result.successCount += taskGroupResult.successCount;
        result.failureCount += taskGroupResult.failureCount;
        that.try(that.afterEach);

        runNext();
      });
    };

    runNext();
  };

  var rootTaskGroup = new TaskGroup("__root__", 0);
  var currentTaskGroup = rootTaskGroup;

  function filterByOnly(arr) {
    function isOnly(el) { return el.only; }
    var filtered = arr.filter(isOnly);

    return filtered.length > 0 ? filtered : arr;
  }

  bdd.run = function () {
    rootTaskGroup.taskGroups = filterByOnly(rootTaskGroup.taskGroups);
    rootTaskGroup.tasks = filterByOnly(rootTaskGroup.tasks);

    rootTaskGroup.run(function (result) {
      reporter.rootTaskFinished(result.failureCount);
    });
  };

  bdd.describe = function (name, definition, only) {
    var newTaskGroup = new TaskGroup(name, currentTaskGroup.depth + 1);

    if (typeof only !== 'undefined') {
      newTaskGroup.only = !!only;
    }

    currentTaskGroup.taskGroups.push(newTaskGroup);

    var previousTaskGroup = currentTaskGroup;
    currentTaskGroup = newTaskGroup;

    definition();

    currentTaskGroup.taskGroups = filterByOnly(currentTaskGroup.taskGroups);
    currentTaskGroup.tasks = filterByOnly(currentTaskGroup.tasks);

    currentTaskGroup = previousTaskGroup;
  };

  bdd.describe.only = function (name, definition) {
    bdd.describe(name, definition, true);
  };

  bdd.describe.skip = function () {
    // Do nothing
  };

  bdd.it = function (name, test, options) {
    var newTask = new Task(name, test);

    if (typeof options !== 'undefined') {
      newTask.only = !!options.only;
      newTask.async = !!options.async;
    }

    currentTaskGroup.tasks.push(newTask);
  };

  bdd.it.only = function (name, test) {
    bdd.it(name, test, {only: true});
  };

  bdd.it.will = function (name, test, only) {
    bdd.it('will ' + name, test, {async: true, only: !!only});
  };

  bdd.it.skip = bdd.it.will.skip = function () {
    // Do nothing
  };

  bdd.it.will.only = function (name, test) {
    bdd.it.will(name, test, true);
  };

  bdd.after = function (fn) {
    currentTaskGroup.after = fn;
  };

  bdd.before = function (fn) {
    currentTaskGroup.before = fn;
  };

  bdd.afterEach = function (fn) {
    currentTaskGroup.afterEach = fn;
  };

  bdd.beforeEach = function (fn) {
    currentTaskGroup.beforeEach = fn;
  };
}

// Mount BDD functions to the scope
exports.mount = function (scope) {
  var bdd = new Bdd();

  for (var i in bdd) {
    scope[i] = bdd[i];
  }
};
