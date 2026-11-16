package io.legado.app.ui.video

/** A UI projection only: source objects and playback indices keep their original identity/order. */
internal data class VideoEpisodeItem<T>(
    val chapter: T,
    val volumeIndex: Int,
    val episodeIndex: Int,
)

internal data class VideoEpisodeGroup<T>(
    val volume: T?,
    val items: List<VideoEpisodeItem<T>>,
)

internal fun <T> videoEpisodeGroups(
    chapters: List<T>,
    isVolume: (T) -> Boolean,
    isPlayableVolume: (T) -> Boolean,
): List<VideoEpisodeGroup<T>> {
    val starts = chapters.indices.filter { isVolume(chapters[it]) }
    if (starts.isEmpty()) {
        return listOf(VideoEpisodeGroup(null, chapters.mapIndexed { index, chapter ->
            VideoEpisodeItem(chapter, 0, index)
        }))
    }
    return starts.mapIndexed { volumeIndex, start ->
        val volume = chapters[start]
        val end = starts.getOrNull(volumeIndex + 1) ?: chapters.size
        val items = if (end > start + 1) {
            (start + 1 until end).map { index ->
                VideoEpisodeItem(chapters[index], volumeIndex, index - start - 1)
            }
        } else if (isPlayableVolume(volume)) {
            // Movie sources can encode each playable line as a volume without children.
            listOf(VideoEpisodeItem(volume, volumeIndex, 0))
        } else emptyList()
        VideoEpisodeGroup(volume, items)
    }
}

/** Search spans the whole work, while normal browsing stays in the selected season/line. */
internal fun <T> filterVideoEpisodes(
    groups: List<VideoEpisodeGroup<T>>,
    volumeIndex: Int,
    query: String,
    descending: Boolean,
    title: (T) -> String,
): List<VideoEpisodeItem<T>> {
    val keyword = query.trim()
    val items = if (keyword.isEmpty()) groups.getOrNull(volumeIndex)?.items.orEmpty() else {
        groups.asSequence().flatMap { it.items.asSequence() }.filter {
            title(it.chapter).contains(keyword, ignoreCase = true) ||
                (it.episodeIndex + 1).toString() == keyword
        }.toList()
    }
    return if (descending) items.asReversed() else items
}
