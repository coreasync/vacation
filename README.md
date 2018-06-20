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

generate
===
```
user=> (use 'app.core :reload)
user=> (run-editor  "test.edn" true false)

```

auxiliary commands and tools
====
```
ffmpeg -i abc.MOV output_%04d.png
mogrify -resize 50% -quality 100 *.png
ffmpeg -framerate 30 -i replay.date.000%03d.tiff -vcodec libx264 -vb 40M -pix_fmt yuv420p myvideo.mp4
for i in range(1,n):
  print "convert left%05d.jpg right%05d.jpg  +append output%05d.jpg"%(i,i,i)


ffmpeg -i abc.MOV -vn -acodec copy output-audio.aac
ffmpeg -framerate 30 -i replay.date.000%03d.tiff -i .../output-audio.aac -vcodec libx264 -vb 40M -pix_fmt yuv420p myvideo.mp4
```

side by side

use https://github.com/ap/rename/blob/master/rename

====
```
ffmpeg -i a.mp4 -s hd720 -c:v libx264 -crf 23 -strict -2 a720.mp4


ffmpeg -i m.mp4 m_%04d.png
ffmpeg -i s.mp4 s_%04d.png
file m1000.png 


 ~/rename -N 1000 's/.*/x$N.png/' *.png
 
mogrify -crop 640x720+220+0 s*.png 
mogrify -crop 640x720+320+0 m*.png
python -c 'for i in range(1,4000): print "convert m%04d.png s%04d.png +append r%04d.png"%(i,i,i)' > s
```
