3.0.1
* Fix Rhino JS runtime support.
* Typo in deprecated warning (by Maxime Thirouin).

## 3.0 “Marquis Andrealphus”
* New parser, which become the fastest ever CSS parser written in JavaScript.
* Parser can now parse declarations and rules in one parent (like in `@page`)
  and nested declarations for plugins like `postcss-nested`.
* Child nodes array is now in `childs` property, instead of `decls` and `rules`.
* `map.inline` and `map.sourcesContent` options are now `true` by default.
* Fix iterators (`each`, `insertAfter`) on children array changes.
* Use previous source map to show origin source of CSS syntax error.
* Use 6to5 ES6 compiler, instead of ES6 Transpiler.
* Use code style for manually added rules from existing rules.
* Use `from` option from previous source map `file` field.
* Set `to` value to `from` if `to` option is missing.
* Use better node source name when missing `from` option.
* Show a syntax error when `;` is missed between declarations.
* Allow to pass `PostCSS` instance or list of plugins to `use()` method.
* Allow to pass `Result` instance to `process()` method.
* Trim Unicode BOM on source maps parsing.
* Parse at-rules without spaces like `@import"file"`.
* Better previous `sourceMappingURL` annotation comment cleaning.
* Do not remove previous `sourceMappingURL` comment on `map.annotation: false`.
* Parse nameless at-rules in Safe Mode.
* Fix source map generation for nodes without source.
* Fix next child `before` if `Root` first child got removed.

## 2.2.6
* Fix map generation for nodes without source (by Josiah Savary).

## 2.2.5
* Fix source map with BOM marker support (by Mohammad Younes).
* Fix source map paths (by Mohammad Younes).

## 2.2.4
* Fix `prepend()` on empty `Root`.

## 2.2.3
* Allow to use object shortcut in `use()` with functions like `autoprefixer`.

## 2.2.2
* Add shortcut to set processors in `use()` via object with `.postcss` property.

## 2.2.1
* Send `opts` from `Processor#process(css, opts)` to processors.

## 2.2 “Marquis Cimeies”
* Use GNU style syntax error messages.
* Add `Node#replace` method.
* Add `CssSyntaxError#reason` property.

## 2.1.2
* Fix UTF-8 support in inline source map.
* Fix source map `sourcesContent` if there is no `from` and `to` options.

## 2.1.1
* Allow to miss `to` and `from` options for inline source maps.
* Add `Node#source.id` if file name is unknown.
* Better detect splitter between rules in CSS concatenation tools.
* Automatically clone node in insert methods.

## 2.1 “King Amdusias”
* Change Traceur ES6 compiler to ES6 Transpiler.
* Show broken CSS line in syntax error.

## 2.0 “King Belial”
* Project was rewritten from CoffeeScript to ES6.
* Add Safe Mode to works with live input or with hacks from legacy code.
* More safer parser to pass all hacks from Browserhacks.com.
* Use real properties instead of magic getter/setter for raw properties.

## 1.0 “Marquis Decarabia”
* Save previous source map for each node to support CSS concatenation
  with multiple previous maps.
* Add `map.sourcesContent` option to add origin content to `sourcesContent`
  inside map.
* Allow to set different place of output map in annotation comment.
* Allow to use arrays and `Root` in `Container#append` and same meth