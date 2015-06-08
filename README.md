# Plugin to display indented multiline HTML in gorilla worksheets.

[Gorilla repl](gorilla-repl.org) is an Clojure interactive environment that follows [Literate programming](https://en.wikipedia.org/wiki/Literate_programming) principles similar to IPython.

While writing an [Enlive tutorial](http://viewer.gorilla-repl.org/view.html?source=github&user=shark8me&repo=clojure-examples&path=src/enliven.cljw) , I did not find a way to display indented html. 
Hence I wrote a plugin for Gorilla-repl that can display indented html. 


To use this plugin in your project, add this dependency in your project.clj

[![Clojars Project](http://clojars.org/gorilla-indented-html-renderer/latest-version.svg)](http://clojars.org/gorilla-indented-html-renderer)

In your worksheet, import thus:
```clojure
(ns your-ns
  (:require [[html-indent.core :refer [view]])
```

To view html

```clojure
(view "<h1><div>abc</div></h1>")
```

which is displayed in the gorilla worksheet as

```raw
<h1>
 <div>abc</div>
</h1>
```


## License

Copyright © 2015 Kiran Karkera

Distributed under the Eclipse Public License version 1.0
