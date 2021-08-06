# Stereo_To_Surround_Audio_Player
#### An android application, which can play a stereo audio by converting stereo stream into surround audio stream (i.e. 7.1 audio), provides a periodic surrounded effect on playback and visualizes input stream through a custom visualizer.  

## Features
- Stereo PCM stream converting into 7.1 stream
- Input stream periodically distributed in 7.1 audio's channels 
- Provided PCM stream through a callback
- Volume independent custom visualizer

## Dependency 
- [iirj](https://github.com/berndporr/iirj) is used for real time audio sample filtering such as low pass, band pass filter in time domain.
