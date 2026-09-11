package com.davidp.simpleweeklyreminders.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which single problem the Reminders-tab banner shows. */
class PermissionNoticeTest {

    @Test
    fun `both on shows nothing`() {
        assertNull(ReminderPermissions(notifications = true, exactAlarms = true).notice())
    }

    @Test
    fun `notifications off`() {
        assertEquals(
            PermissionNotice.NOTIFICATIONS_OFF,
            ReminderPermissions(notifications = false, exactAlarms = true).notice()
        )
    }

    @Test
    fun `exact alarms off`() {
        assertEquals(
            PermissionNotice.EXACT_ALARMS_OFF,
            ReminderPermissions(notifications = true, exactAlarms = false).notice()
        )
    }

    @Test
    fun `both off asks for notifications first`() {
        // An on-time alarm is pointless with nothing to show, so notifications outrank it
        assertEquals(
            PermissionNotice.NOTIFICATIONS_OFF,
            ReminderPermissions(notifications = false, exactAlarms = false).notice()
        )
    }
}
