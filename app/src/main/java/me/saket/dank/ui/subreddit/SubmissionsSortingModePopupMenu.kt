package me.saket.dank.ui.subreddit

import android.content.Context
import android.support.v7.widget.PopupMenu
import android.view.Gravity
import android.view.View
import me.saket.dank.R
import me.saket.dank.ui.submission.SortingAndTimePeriod
import net.dean.jraw.models.SubredditSort.BEST
import net.dean.jraw.models.SubredditSort.CONTROVERSIAL
import net.dean.jraw.models.SubredditSort.HOT
import net.dean.jraw.models.SubredditSort.NEW
import net.dean.jraw.models.SubredditSort.RISING
import net.dean.jraw.models.SubredditSort.TOP
import net.dean.jraw.models.TimePeriod.ALL
import net.dean.jraw.models.TimePeriod.DAY
import net.dean.jraw.models.TimePeriod.HOUR
import net.dean.jraw.models.TimePeriod.MONTH
import net.dean.jraw.models.TimePeriod.WEEK
import net.dean.jraw.models.TimePeriod.YEAR

class SubmissionsSortingModePopupMenu(
    private val context: Context,
    anchorView: View
) : PopupMenu(context, anchorView, Gravity.NO_GRAVITY, 0, R.style.DankPopupMenu_SubmissionSortingMode) {

  private lateinit var onSortingModeSelectListener: OnSortingModeSelectListener

  interface OnSortingModeSelectListener {
    fun onSortingModeSelect(sortingAndTimePeriod: SortingAndTimePeriod)
  }

  init {
    val map = mapOf<Int, SortingAndTimePeriod>(
        R.id.action_subreddit_sorting_best to SortingAndTimePeriod.create(BEST),
        R.id.action_subreddit_sorting_hot to SortingAndTimePeriod.create(HOT),
        R.id.action_subreddit_sorting_new to SortingAndTimePeriod.create(NEW),
        R.id.action_subreddit_sorting_rising to SortingAndTimePeriod.create(RISING),
        R.id.action_subreddit_sorting_controversial_hour to SortingAndTimePeriod.create(CONTROVERSIAL, HOUR),
        R.id.action_subreddit_sorting_controversial_day to SortingAndTimePeriod.create(CONTROVERSIAL, DAY),
        R.id.action_subreddit_sorting_controversial_week to SortingAndTimePeriod.create(CONTROVERSIAL, WEEK),
        R.id.action_subreddit_sorting_controversial_month to SortingAndTimePeriod.create(CONTROVERSIAL, MONTH),
        R.id.action_subreddit_sorting_controversial_year to SortingAndTimePeriod.create(CONTROVERSIAL, YEAR),
        R.id.action_subreddit_sorting_controversial_alltime to SortingAndTimePeriod.create(CONTROVERSIAL, ALL),
        R.id.action_subreddit_sorting_top_hour to SortingAndTimePeriod.create(TOP, HOUR),
        R.id.action_subreddit_sorting_top_day to SortingAndTimePeriod.create(TOP, DAY),
        R.id.action_subreddit_sorting_top_week to SortingAndTimePeriod.create(TOP, WEEK),
        R.id.action_subreddit_sorting_top_month to SortingAndTimePeriod.create(TOP, MONTH),
        R.id.action_subreddit_sorting_top_year to SortingAndTimePeriod.create(TOP, YEAR),
        R.id.action_subreddit_sorting_top_alltime to SortingAndTimePeriod.create(TOP, ALL)
    )

    setOnMenuItemClickListener { menuItem ->
      val itemItemId = menuItem.itemId

      when (itemItemId) {
        R.id.action_subreddit_sorting_controversial,
        R.id.action_subreddit_sorting_top -> {
          // Submenu.
          false
        }

        else -> {
          val sortingAndTimePeriod = map[itemItemId]
              ?: throw AssertionError("Unknown sorting and time period: " + context.resources.getResourceEntryName(itemItemId))
          onSortingModeSelectListener.onSortingModeSelect(sortingAndTimePeriod)
          true
        }
      }
    }
  }

  fun setOnSortingModeSelectListener(listener: OnSortingModeSelectListener) {
    onSortingModeSelectListener = listener
  }

  // NOTE: Keep this in sync with CommentSortingModePopupMenu.
  fun highlightActiveSortingAndTimePeriod(highlighted: SortingAndTimePeriod) {
    val highlightedSorting = context.getString(highlighted.sortingDisplayTextRes)
    val highlightedTimePeriod = context.getString(highlighted.timePeriodDisplayTextRes)

    for (i in 0 until menu.size()) {
      val menuItem = menu.getItem(i)

      val isSortActive = highlightedSorting.equals(menuItem.title.toString(), ignoreCase = true)
      if (isSortActive) {
        if (menuItem.hasSubMenu()) {
          val subMenu = menuItem.subMenu
          for (j in 0 until subMenu.size()) {
            val subMenuItem = subMenu.getItem(j)
            val isTimePeriodActive = highlightedTimePeriod.equals(subMenuItem.title.toString(), ignoreCase = true)
            if (isTimePeriodActive) {
              // PopupMenu won't let us apply spans. So we'll just gray the selected items in the meanwhile.
              subMenuItem.isEnabled = false
            }
          }

        } else {
          menuItem.isEnabled = false
        }
      }
    }
  }
}
