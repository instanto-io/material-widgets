# Media showcase assets

The original Media view and UiBinder template come from the pinned
[gmd-core-demo](https://github.com/GwtMaterialDesign/gmd-core-demo/tree/63528c811e5e91a6bdbd5b523748d47251cc89f9/src/main/java/gmd/core/demo/client/application/page/media).
Its image URLs are mapped to local copies, including those in displayed XML
snippets. The six files were retrieved unchanged on 13 September 2026 from
[GWT Material's demo images](https://github.com/GwtMaterialDesign/gwt-material-demo/tree/e126ac93020d78bb997a9048afeff61110d03151/images):
`candy-icons.gif`, `md-card-ripple.gif`, `blue.jpg`, `grey.jpg`, `light-blue.jpg`
and `pink.jpg`. They remain credited to the upstream project and contributors.

The [original YouTube embed](https://www.youtube.com/watch?v=Q8TXgCzxEnw) is
replaced by the local `media/player.html`. The iframe plays our own four-second silent geometric
animation: 320 × 180, H.264, 30 frames per second. It contains no external footage
or audio; its SVG poster depicts the first frame. The sample was generated once
using macOS AVFoundation; generating video is not a build step or dependency. Maven copies these static assets.

`MaterialVideo` provides the responsive iframe. Playback, pause and seek belong
to the browser's native video player inside it. Tests of this fixture establish
embedding and browser playback, not YouTube integration or a Material player API.

SHA-256 checksums of the maintained assets:

```text
b5be895ebfbf7061ac3c17a0209cd1dd82229ea0adcc13a04a78fe0759c75438  showcase-teavm/src/site/media/images/blue.jpg
75aad1f2952a6d04f4df430f594773b33e66060ad3242e0ffbbf88caf1e280e1  showcase-teavm/src/site/media/images/candy-icons.gif
7a8cf5dbf0083fb087a1712b79dd22bfc404b0bda6c81dadf308988bc65f6be6  showcase-teavm/src/site/media/images/grey.jpg
e44d0593c1bd9df6f9b49f17e7b51ad177bea02cfe3e03fd8c8a17d9fd097101  showcase-teavm/src/site/media/images/light-blue.jpg
2cdb69193c5305a7c4dc96477bc6f38457a3b66e8fdf83bf0ca8d5e58fa4cb24  showcase-teavm/src/site/media/images/md-card-ripple.gif
8d6f573a44d9a68e01cd7b6b82a409c117688ed3c7eaf11a133c286b5df3798e  showcase-teavm/src/site/media/images/pink.jpg
f5ddf396697990e0b03c8a2dbd4af995b21964531ff9371649edd51ca76766db  showcase-teavm/src/site/media/sample.mp4
d049d5a31a3e3ecf83b7e01bd474516a55e97f8b04c7df07db65065e84e82c55  showcase-teavm/src/site/media/sample-poster.svg
```
