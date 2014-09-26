var gulp = require('gulp');
var jshint = require('gulp-jshint');
var jshintStylish = require('jshint-stylish');

gulp.task('jshint', function () {
  return gulp.src([
      './gulpfile.js',
      './plugins/**/*.js'
    ])
    .pipe(jshint())
    .pipe(jshint.reporter(jshintStylish));
});

gulp.task('default', ['jshint']);
