start
===
```
lein repl
```

create project
===
```
user=> (mk-project "test.edn" ".../fullimgs" [1920 1080] "../thumbimgs" 2)
```

edit
===
```
user=> (use 'app.core :reload)
user=> (run-editor  "test.edn" false false)

```

auxiliary commands and tools
====
```
ffmpeg -i abc.MOV output_%04d.png
mogrify -resize 50% -quality 100 *.png
ffmpeg -framerate 30 -i replay.date.000%03d.tiff -vcodec libx264 -vb 40M -pix_fmt yuv420p myvideo.mp4
for i in range(1,n):
  print "convert left%05d.jpg right%05d.jpg  +append output%05d.jpg"%(i,i,i)
```
