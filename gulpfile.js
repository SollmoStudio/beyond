var gulp = require('gulp');
var eslint = require('gulp-eslint');

gulp.task('eslint', function () {
  return gulp.src([
      './core/public/js_lib/**/*.js',
      './gulpfile.js',
      './plugins/**/*.js'
    ])
    .pipe(eslint())
    .pipe(eslint.format())
    .pipe(eslint.failOnError());
});

gulp.task('default', ['eslint']);
