# read-this

![](http://i2.kym-cdn.com/photos/images/facebook/001/091/387/b91.jpg)

Weekend project: A curl-app - something like wttr.in - but for news!

Why would you ever leave your comfy terminal if you could easily
curl in the best parts of the internet right there?

Who needs fancy formatting, or style?

Rhetorical questions, what are those about?

Think of ```read-this``` it as slow, clunky, rss-ish terminal feeder-reader 

For me, by me!

## Routes and Sources

The full list of defined routes are easy to see in ```handler/defroutes```, but the gist of it is that you get news either by category or by source:

### Categories

Get a selection of links and titles by hitting ```GET /{{category}}``` where the categories are:

- lit: nyrb, 3am, paris-review
- tech: hacker-news
- news: cbc
- ball: r-nba, the-ringer

### Sources

Individual sources can also be hit at ```GET /from?source={{source}}```
The sources are listed above under their respective categories.

Using sources, you can also read a given article by providing the query-param for the index number like

```GET /from?source=3am&read=2```

### Adding Sources

It's pretty easy to add a source, and all sources just live in a clojure map, so just do something like:

```clojure
(assoc site-data :paris-review {:title-selector [:main :ul :li :article :h1 :a]
                                :url (URL. "https://www.theparisreview.org/blog/") 
                                :protocol :https
                                :p-selector [:main :p]}
```

and boom, you're reading the paris review, comrade.

## Running

To start a web server for the application, run:

    lein ring server-headless

## Future Features, or more aptly 'project aspirations that will probably die in the next week or so as this gets swept under the rug'

- I've implemented a grubby bit of colorization for max terminal excitement, but it also adds those ansi ecape bits to the browser rendered page, so the formatting is a little off.. that's gottab e cleaned up
- The html-wrapping should also be switched on and off based on the ua header.
- Most of the sources (read 'all of the sources other than the lit ones') have yet to be implemented as readable in the terminal. For accumulators like reddit and hacker-news, this isn't an easy fix, since we don't know the DOM structure of the linked page, but the others should be fixed soon.
- All of the data is gotten, filtered, rendered, formatted, on each request. This is a stupid, approach and the build lists and articles should be rendered and cached a couple times a day to make this faster. That'll change one day, maybe. It's still easier and faster to find an article this way than surfin the ol' web blindly though...
- Cutting lines off at a given length is good, but recognizing that you're cutting one letter off of a given word would be so much better.

## License

Copyright © 2017 Calin Fraser
