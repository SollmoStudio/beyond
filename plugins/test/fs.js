var assert = require('assert');
var fs = require('fs');
var wait = require('test-helper').wait;
require('bdd').mount(this);

describe('file-system', function () {
  describe('#readFile()', function () {
    it('return the data of the file.', function () {
      var data = wait(fs.readFile('./plugins/test/assets/fs/beyond'));
      assert.equal(data, 'Beyond framework\n');
    });

    it('return the data of the file with a specific encoding.', function () {
      var data = wait(fs.readFile('./plugins/test/assets/fs/hello', {encoding: 'EUC-KR'}));
      assert.equal(data, '안녕, 세계!\n');
    });

    it('fire an exception when there is no file.', function () {
      try {
        wait(fs.readFile('./plugins/test/assets/fs/nothing'));
        throw new Error('no error!');
      } catch (e) {
        assert.equal(e.message, "java.io.IOException: Can't find the file");
      }
    });
  });

  describe('#writeFile()', function () {
    var testFilePath = "./plugins/test/assets/fs/test";

    function exec(command) {
      return java.lang.Runtime.getRuntime().exec(command).waitFor();
    }

    function removeTestFile() {
      // Remove the file we used or will use
      exec("rm " + testFilePath);
    }
    beforeEach(removeTestFile);
    afterEach(removeTestFile);

    function contentEqualTo(expected, options) {
      var future;
      if (typeof options === 'undefined') {
        future = fs.readFile(testFilePath);
      } else {
        future = fs.readFile(testFilePath, options);
      }

      var data = wait(future);
      assert.equal(data, expected);
    }

    function permissionEqualTo(expected) {
      var error = !!exec(["sh", "-c", "find " + testFilePath + " -perm " + expected + " | grep '.*'"]);
      assert.equal(error, false);
    }

    it('write data to a file.', function () {
      wait(fs.writeFile(testFilePath, "Hello, world!"));
      contentEqualTo("Hello, world!");
    });

    it('write data to a file with encoding.', function () {
      wait(fs.writeFile(testFilePath, "안녕, 세계!", {encoding: 'EUC-KR'}));
      contentEqualTo("안녕, 세계!", {encoding: 'EUC-KR'});
    });

    it('write data to a file with mode.', function () {
      wait(fs.writeFile(testFilePath, "Hello, world!", {mode: 0765}));
      permissionEqualTo('765');
    });

    it('write data to a file with encoding and mode.', function () {
      wait(fs.writeFile(testFilePath, "안녕, 세계!", {encoding: 'EUC-KR', mode: 0657}));
      permissionEqualTo('657');
      contentEqualTo("안녕, 세계!", {encoding: 'EUC-KR'});
    });
  });

  describe('#readdir()', function () {
    var future;
    beforeEach(function () {
      future = fs.readdir('./plugins/test/assets/fs');
    });

    function findWithName(files, name) {
      return files.filter(function (file) {
        return file.name === name;
      })[0];
    }

    it('return the list of files with the correct length.', function () {
      var files = wait(future);
      assert.equal(files.length, 3);
    });

    it('return the list of files containing static files', function () {
      var files = wait(future);
      assert.equal(findWithName(files, 'hello').name, 'hello');
      assert.equal(findWithName(files, 'hello').path, './plugins/test/assets/fs/hello');
      assert.equal(findWithName(files, 'hello').isFile, true);
      assert.equal(findWithName(files, 'hello').isDirectory, false);

      assert.equal(findWithName(files, 'beyond').name, 'beyond');
      assert.equal(findWithName(files, 'beyond').path, './plugins/test/assets/fs/beyond');
      assert.equal(findWithName(files, 'beyond').isFile, true);
      assert.equal(findWithName(files, 'beyond').isDirectory, false);
    });

    it('return the list of files containing the directory', function () {
      var files = wait(future);
      assert.equal(findWithName(files, 'dir').name, 'dir');
      assert.equal(findWithName(files, 'dir').path, './plugins/test/assets/fs/dir');
      assert.equal(findWithName(files, 'dir').isFile, false);
      assert.equal(findWithName(files, 'dir').isDirectory, true);
    });
  });

  describe('#read()', function () {
    it('returns a file object.', function () {
      var file = fs.read('./plugins/test/assets/fs/hello');
      assert.equal(typeof file, 'object');
    });

    it('returns an object containing its information.', function () {
      var file = fs.read('./plugins/test/assets/fs/hello');
      assert.equal(file.name, 'hello');
      assert.equal(file.path, './plugins/test/assets/fs/hello');
      assert.equal(file.isFile, true);
      assert.equal(file.isDirectory, false);
    });

    it('returns an object representing a directory.', function () {
      var file = fs.read('./plugins/test/assets/fs/dir');
      assert.equal(file.name, 'dir');
      assert.equal(file.path, './plugins/test/assets/fs/dir');
      assert.equal(file.isFile, false);
      assert.equal(file.isDirectory, true);
    });
  });
});
