package ru.muz.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ru.muz.settings.IconPack

internal val LocalIconPack = staticCompositionLocalOf { IconPack.ROUNDED }

@Composable
internal fun Icon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    androidx.compose.material3.Icon(packIcon(imageVector, LocalIconPack.current), contentDescription?.let(::tr), modifier, tint)
}

internal fun packIcon(original: ImageVector, pack: IconPack): ImageVector {
    if (pack == IconPack.ROUNDED) return original
    return when (original.name.substringAfterLast('.')) {
        "AccountCircle" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.AccountCircle; IconPack.SHARP -> Icons.Sharp.AccountCircle; IconPack.TWO_TONE -> Icons.TwoTone.AccountCircle; else -> original }
        "ArrowBack" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.ArrowBack; IconPack.SHARP -> Icons.Sharp.ArrowBack; IconPack.TWO_TONE -> Icons.TwoTone.ArrowBack; else -> original }
        "AspectRatio" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.AspectRatio; IconPack.SHARP -> Icons.Sharp.AspectRatio; IconPack.TWO_TONE -> Icons.TwoTone.AspectRatio; else -> original }
        "BarChart" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.BarChart; IconPack.SHARP -> Icons.Sharp.BarChart; IconPack.TWO_TONE -> Icons.TwoTone.BarChart; else -> original }
        "Bedtime" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Bedtime; IconPack.SHARP -> Icons.Sharp.Bedtime; IconPack.TWO_TONE -> Icons.TwoTone.Bedtime; else -> original }
        "Block" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Block; IconPack.SHARP -> Icons.Sharp.Block; IconPack.TWO_TONE -> Icons.TwoTone.Block; else -> original }
        "Check" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Check; IconPack.SHARP -> Icons.Sharp.Check; IconPack.TWO_TONE -> Icons.TwoTone.Check; else -> original }
        "ChevronRight" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.ChevronRight; IconPack.SHARP -> Icons.Sharp.ChevronRight; IconPack.TWO_TONE -> Icons.TwoTone.ChevronRight; else -> original }
        "Close" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Close; IconPack.SHARP -> Icons.Sharp.Close; IconPack.TWO_TONE -> Icons.TwoTone.Close; else -> original }
        "CloudOff" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.CloudOff; IconPack.SHARP -> Icons.Sharp.CloudOff; IconPack.TWO_TONE -> Icons.TwoTone.CloudOff; else -> original }
        "Code" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Code; IconPack.SHARP -> Icons.Sharp.Code; IconPack.TWO_TONE -> Icons.TwoTone.Code; else -> original }
        "Colorize" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Colorize; IconPack.SHARP -> Icons.Sharp.Colorize; IconPack.TWO_TONE -> Icons.TwoTone.Colorize; else -> original }
        "ContentCopy" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.ContentCopy; IconPack.SHARP -> Icons.Sharp.ContentCopy; IconPack.TWO_TONE -> Icons.TwoTone.ContentCopy; else -> original }
        "ContentPaste" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.ContentPaste; IconPack.SHARP -> Icons.Sharp.ContentPaste; IconPack.TWO_TONE -> Icons.TwoTone.ContentPaste; else -> original }
        "Dashboard" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Dashboard; IconPack.SHARP -> Icons.Sharp.Dashboard; IconPack.TWO_TONE -> Icons.TwoTone.Dashboard; else -> original }
        "DataSaverOn" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.DataSaverOn; IconPack.SHARP -> Icons.Sharp.DataSaverOn; IconPack.TWO_TONE -> Icons.TwoTone.DataSaverOn; else -> original }
        "DeleteSweep" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.DeleteSweep; IconPack.SHARP -> Icons.Sharp.DeleteSweep; IconPack.TWO_TONE -> Icons.TwoTone.DeleteSweep; else -> original }
        "Download" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Download; IconPack.SHARP -> Icons.Sharp.Download; IconPack.TWO_TONE -> Icons.TwoTone.Download; else -> original }
        "DownloadForOffline" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.DownloadForOffline; IconPack.SHARP -> Icons.Sharp.DownloadForOffline; IconPack.TWO_TONE -> Icons.TwoTone.DownloadForOffline; else -> original }
        "Equalizer" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Equalizer; IconPack.SHARP -> Icons.Sharp.Equalizer; IconPack.TWO_TONE -> Icons.TwoTone.Equalizer; else -> original }
        "ExpandLess" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.ExpandLess; IconPack.SHARP -> Icons.Sharp.ExpandLess; IconPack.TWO_TONE -> Icons.TwoTone.ExpandLess; else -> original }
        "ExpandMore" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.ExpandMore; IconPack.SHARP -> Icons.Sharp.ExpandMore; IconPack.TWO_TONE -> Icons.TwoTone.ExpandMore; else -> original }
        "Favorite" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Favorite; IconPack.SHARP -> Icons.Sharp.Favorite; IconPack.TWO_TONE -> Icons.TwoTone.Favorite; else -> original }
        "FavoriteBorder" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.FavoriteBorder; IconPack.SHARP -> Icons.Sharp.FavoriteBorder; IconPack.TWO_TONE -> Icons.TwoTone.FavoriteBorder; else -> original }
        "FileDownload" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.FileDownload; IconPack.SHARP -> Icons.Sharp.FileDownload; IconPack.TWO_TONE -> Icons.TwoTone.FileDownload; else -> original }
        "FileUpload" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.FileUpload; IconPack.SHARP -> Icons.Sharp.FileUpload; IconPack.TWO_TONE -> Icons.TwoTone.FileUpload; else -> original }
        "FormatAlignCenter" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.FormatAlignCenter; IconPack.SHARP -> Icons.Sharp.FormatAlignCenter; IconPack.TWO_TONE -> Icons.TwoTone.FormatAlignCenter; else -> original }
        "FormatAlignLeft" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.FormatAlignLeft; IconPack.SHARP -> Icons.Sharp.FormatAlignLeft; IconPack.TWO_TONE -> Icons.TwoTone.FormatAlignLeft; else -> original }
        "FormatBold" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.FormatBold; IconPack.SHARP -> Icons.Sharp.FormatBold; IconPack.TWO_TONE -> Icons.TwoTone.FormatBold; else -> original }
        "FormatListBulleted" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.FormatListBulleted; IconPack.SHARP -> Icons.Sharp.FormatListBulleted; IconPack.TWO_TONE -> Icons.TwoTone.FormatListBulleted; else -> original }
        "FormatQuote" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.FormatQuote; IconPack.SHARP -> Icons.Sharp.FormatQuote; IconPack.TWO_TONE -> Icons.TwoTone.FormatQuote; else -> original }
        "GraphicEq" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.GraphicEq; IconPack.SHARP -> Icons.Sharp.GraphicEq; IconPack.TWO_TONE -> Icons.TwoTone.GraphicEq; else -> original }
        "Headphones" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Headphones; IconPack.SHARP -> Icons.Sharp.Headphones; IconPack.TWO_TONE -> Icons.TwoTone.Headphones; else -> original }
        "History" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.History; IconPack.SHARP -> Icons.Sharp.History; IconPack.TWO_TONE -> Icons.TwoTone.History; else -> original }
        "Home" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Home; IconPack.SHARP -> Icons.Sharp.Home; IconPack.TWO_TONE -> Icons.TwoTone.Home; else -> original }
        "Image" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Image; IconPack.SHARP -> Icons.Sharp.Image; IconPack.TWO_TONE -> Icons.TwoTone.Image; else -> original }
        "ImportExport" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.ImportExport; IconPack.SHARP -> Icons.Sharp.ImportExport; IconPack.TWO_TONE -> Icons.TwoTone.ImportExport; else -> original }
        "Info" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Info; IconPack.SHARP -> Icons.Sharp.Info; IconPack.TWO_TONE -> Icons.TwoTone.Info; else -> original }
        "InsertLink" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.InsertLink; IconPack.SHARP -> Icons.Sharp.InsertLink; IconPack.TWO_TONE -> Icons.TwoTone.InsertLink; else -> original }
        "KeyboardArrowDown" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.KeyboardArrowDown; IconPack.SHARP -> Icons.Sharp.KeyboardArrowDown; IconPack.TWO_TONE -> Icons.TwoTone.KeyboardArrowDown; else -> original }
        "KeyboardArrowUp" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.KeyboardArrowUp; IconPack.SHARP -> Icons.Sharp.KeyboardArrowUp; IconPack.TWO_TONE -> Icons.TwoTone.KeyboardArrowUp; else -> original }
        "LibraryMusic" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.LibraryMusic; IconPack.SHARP -> Icons.Sharp.LibraryMusic; IconPack.TWO_TONE -> Icons.TwoTone.LibraryMusic; else -> original }
        "LocalFireDepartment" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.LocalFireDepartment; IconPack.SHARP -> Icons.Sharp.LocalFireDepartment; IconPack.TWO_TONE -> Icons.TwoTone.LocalFireDepartment; else -> original }
        "Lock" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Lock; IconPack.SHARP -> Icons.Sharp.Lock; IconPack.TWO_TONE -> Icons.TwoTone.Lock; else -> original }
        "Lyrics" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Lyrics; IconPack.SHARP -> Icons.Sharp.Lyrics; IconPack.TWO_TONE -> Icons.TwoTone.Lyrics; else -> original }
        "MenuBook" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.MenuBook; IconPack.SHARP -> Icons.Sharp.MenuBook; IconPack.TWO_TONE -> Icons.TwoTone.MenuBook; else -> original }
        "MoreHoriz" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.MoreHoriz; IconPack.SHARP -> Icons.Sharp.MoreHoriz; IconPack.TWO_TONE -> Icons.TwoTone.MoreHoriz; else -> original }
        "MoreVert" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.MoreVert; IconPack.SHARP -> Icons.Sharp.MoreVert; IconPack.TWO_TONE -> Icons.TwoTone.MoreVert; else -> original }
        "MotionPhotosOff" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.MotionPhotosOff; IconPack.SHARP -> Icons.Sharp.MotionPhotosOff; IconPack.TWO_TONE -> Icons.TwoTone.MotionPhotosOff; else -> original }
        "Movie" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Movie; IconPack.SHARP -> Icons.Sharp.Movie; IconPack.TWO_TONE -> Icons.TwoTone.Movie; else -> original }
        "MusicNote" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.MusicNote; IconPack.SHARP -> Icons.Sharp.MusicNote; IconPack.TWO_TONE -> Icons.TwoTone.MusicNote; else -> original }
        "OpenInNew" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.OpenInNew; IconPack.SHARP -> Icons.Sharp.OpenInNew; IconPack.TWO_TONE -> Icons.TwoTone.OpenInNew; else -> original }
        "Palette" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Palette; IconPack.SHARP -> Icons.Sharp.Palette; IconPack.TWO_TONE -> Icons.TwoTone.Palette; else -> original }
        "Pause" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Pause; IconPack.SHARP -> Icons.Sharp.Pause; IconPack.TWO_TONE -> Icons.TwoTone.Pause; else -> original }
        "Person" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Person; IconPack.SHARP -> Icons.Sharp.Person; IconPack.TWO_TONE -> Icons.TwoTone.Person; else -> original }
        "PlayArrow" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.PlayArrow; IconPack.SHARP -> Icons.Sharp.PlayArrow; IconPack.TWO_TONE -> Icons.TwoTone.PlayArrow; else -> original }
        "PlayCircle" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.PlayCircle; IconPack.SHARP -> Icons.Sharp.PlayCircle; IconPack.TWO_TONE -> Icons.TwoTone.PlayCircle; else -> original }
        "PlaylistAdd" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.PlaylistAdd; IconPack.SHARP -> Icons.Sharp.PlaylistAdd; IconPack.TWO_TONE -> Icons.TwoTone.PlaylistAdd; else -> original }
        "QueueMusic" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.QueueMusic; IconPack.SHARP -> Icons.Sharp.QueueMusic; IconPack.TWO_TONE -> Icons.TwoTone.QueueMusic; else -> original }
        "QueuePlayNext" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.QueuePlayNext; IconPack.SHARP -> Icons.Sharp.QueuePlayNext; IconPack.TWO_TONE -> Icons.TwoTone.QueuePlayNext; else -> original }
        "Refresh" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Refresh; IconPack.SHARP -> Icons.Sharp.Refresh; IconPack.TWO_TONE -> Icons.TwoTone.Refresh; else -> original }
        "Repeat" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Repeat; IconPack.SHARP -> Icons.Sharp.Repeat; IconPack.TWO_TONE -> Icons.TwoTone.Repeat; else -> original }
        "RepeatOne" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.RepeatOne; IconPack.SHARP -> Icons.Sharp.RepeatOne; IconPack.TWO_TONE -> Icons.TwoTone.RepeatOne; else -> original }
        "Restore" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Restore; IconPack.SHARP -> Icons.Sharp.Restore; IconPack.TWO_TONE -> Icons.TwoTone.Restore; else -> original }
        "Save" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Save; IconPack.SHARP -> Icons.Sharp.Save; IconPack.TWO_TONE -> Icons.TwoTone.Save; else -> original }
        "Search" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Search; IconPack.SHARP -> Icons.Sharp.Search; IconPack.TWO_TONE -> Icons.TwoTone.Search; else -> original }
        "Settings" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Settings; IconPack.SHARP -> Icons.Sharp.Settings; IconPack.TWO_TONE -> Icons.TwoTone.Settings; else -> original }
        "Share" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Share; IconPack.SHARP -> Icons.Sharp.Share; IconPack.TWO_TONE -> Icons.TwoTone.Share; else -> original }
        "Shuffle" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Shuffle; IconPack.SHARP -> Icons.Sharp.Shuffle; IconPack.TWO_TONE -> Icons.TwoTone.Shuffle; else -> original }
        "SkipNext" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.SkipNext; IconPack.SHARP -> Icons.Sharp.SkipNext; IconPack.TWO_TONE -> Icons.TwoTone.SkipNext; else -> original }
        "SkipPrevious" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.SkipPrevious; IconPack.SHARP -> Icons.Sharp.SkipPrevious; IconPack.TWO_TONE -> Icons.TwoTone.SkipPrevious; else -> original }
        "SlowMotionVideo" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.SlowMotionVideo; IconPack.SHARP -> Icons.Sharp.SlowMotionVideo; IconPack.TWO_TONE -> Icons.TwoTone.SlowMotionVideo; else -> original }
        "Speed" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Speed; IconPack.SHARP -> Icons.Sharp.Speed; IconPack.TWO_TONE -> Icons.TwoTone.Speed; else -> original }
        "Sync" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Sync; IconPack.SHARP -> Icons.Sharp.Sync; IconPack.TWO_TONE -> Icons.TwoTone.Sync; else -> original }
        "SystemUpdate" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.SystemUpdate; IconPack.SHARP -> Icons.Sharp.SystemUpdate; IconPack.TWO_TONE -> Icons.TwoTone.SystemUpdate; else -> original }
        "TextFields" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.TextFields; IconPack.SHARP -> Icons.Sharp.TextFields; IconPack.TWO_TONE -> Icons.TwoTone.TextFields; else -> original }
        "TrendingDown" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.TrendingDown; IconPack.SHARP -> Icons.Sharp.TrendingDown; IconPack.TWO_TONE -> Icons.TwoTone.TrendingDown; else -> original }
        "TrendingUp" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.TrendingUp; IconPack.SHARP -> Icons.Sharp.TrendingUp; IconPack.TWO_TONE -> Icons.TwoTone.TrendingUp; else -> original }
        "Tune" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Tune; IconPack.SHARP -> Icons.Sharp.Tune; IconPack.TWO_TONE -> Icons.TwoTone.Tune; else -> original }
        "Visibility" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.Visibility; IconPack.SHARP -> Icons.Sharp.Visibility; IconPack.TWO_TONE -> Icons.TwoTone.Visibility; else -> original }
        "VisibilityOff" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.VisibilityOff; IconPack.SHARP -> Icons.Sharp.VisibilityOff; IconPack.TWO_TONE -> Icons.TwoTone.VisibilityOff; else -> original }
        "WorkspacePremium" -> when (pack) { IconPack.OUTLINED -> Icons.Outlined.WorkspacePremium; IconPack.SHARP -> Icons.Sharp.WorkspacePremium; IconPack.TWO_TONE -> Icons.TwoTone.WorkspacePremium; else -> original }
        else -> original
    }
}
