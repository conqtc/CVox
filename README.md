# CVox
Meet CVox, a client mobile app for classic audio book website Librivox (https://librivox.org/) running on Oreo (API 26).

## User Interface
This app targets from Android Marshmallow (API 23) to Oreo (API 26) and applies some Material Design guidelines.
* SearchView using configuration in manifest files which define “android.app.default_searchable” and “android.app.searchable” tag
* Alert-themed activities
* Animation for activity display and image loading

## Networking Handling and Concurrent Programming
*	Retrieve the list of new audio book from archive.org (RSS)
*	Retrieve the list of new podcast from librivox.org (RSS)
*	Retrieve the list of new blog feeds from librivox (RSS)
*	Lookup book’s details and list of tracks information based on book’ id (JSON)
* **Increase async task priority by executing in executor thread pool**

## Asynchronous list image loading
* Asynchronously download and load bitmap to ImageView by matching "tag"
* Cancel previous download task for new re-use view (view holder pattern)

## Media Streaming
Implement a media service to stream audio tracks (based on https://www.sitepoint.com/a-step-by-step-guide-to-building-an-android-audio-player-app/).

## Media Notification
Implement Notification Channel for Oreo compatibility

## Data Parsing (XML and JSON)
CVox uses AsynTask to parse:
* XML (using XmlPullParser)
*	JSON (using JsonReader)
 
*Screenshots*
### Main
![Main](https://raw.githubusercontent.com/conqtc/CVox/master/Screenshots/1_main_activity.png)

### Podcast
![Podcast](https://raw.githubusercontent.com/conqtc/CVox/master/Screenshots/2_podcast.png)

### Blog
![Blog](https://raw.githubusercontent.com/conqtc/CVox/master/Screenshots/3_blog.png)

### Comment
![Comment](https://raw.githubusercontent.com/conqtc/CVox/master/Screenshots/4_comment.png)

### Book detail
![Book detail](https://raw.githubusercontent.com/conqtc/CVox/master/Screenshots/5_book_detail.png)

### Book detail (landscape)
![Book detail (landscape)](https://raw.githubusercontent.com/conqtc/CVox/master/Screenshots/6_book_detail_landscape.png)

### Buffering
![Buffering](https://raw.githubusercontent.com/conqtc/CVox/master/Screenshots/7_buffering.png)

### Playing (lanscape) 
![Playing](https://raw.githubusercontent.com/conqtc/CVox/master/Screenshots/8_landscape.png)

### Playing
![Playing](https://raw.githubusercontent.com/conqtc/CVox/master/Screenshots/9_playing.png)

### Search
![Search](https://raw.githubusercontent.com/conqtc/CVox/master/Screenshots/10_search.png)
