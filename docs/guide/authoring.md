# Authoring

> :warning:
>
> If you are a musician with minimal programming experience, you should consider this is the end of the guide for now.
>
> We are actively creating a `bach` editor so that anyone can author and playback `bach` tracks in the browser.
>
> For the time being, track playback is only accessible to [developers](dev).

Now that you are familiar with the fundamentals, we can begin putting `bach` to practical use by authoring some tracks.

Regardless of your level of familiarity or expertise, the ideal way to write `bach` tracks is to always start off with a similar example.

It's much better to start off with a block of marble and carve out a sculpture than to slowly build up a sculpture piece by piece.

That is why we provide an open-source collection of [examples tracks](#examples) for you to copy and modify to your liking.

But before we can even begin to make use of these examples (let alone change or build upon them), we must become familiar with the tools available to us.

### Tools

Today, all of the tooling for `bach` is programmatic. In other words, `bach` can easily be used by programmers, but not so easily by musicians since essential high-level tools for `bach` are still being developed.

For instsance, eventually we will provide an open-source `bach` web editor that will allow you to author and play `bach` tracks entirely in the browser.

Until that point, all of `bach`'s tooling is highly technical and built for software engineers.

### Audio

> :warning: Audio playback can currently only be achieved programmatically via [`gig`](dev#gig).

You have a `bach` track written, so how do we associate and synchronize it with audio?

The first thing to note is that, in order to keep `bach` simple and focused, `bach` doesn't explicitly concern itself with audio data.

By taking this approach it allows `bach` to rhythmically align with music produced by a human or to generate music on a computer.

On a practical level, this means it's up to your editor or application to associate audio data with your `bach` tracks.
