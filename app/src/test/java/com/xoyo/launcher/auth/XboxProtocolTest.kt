package com.xoyo.launcher.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XboxProtocolTest {

    @Test
    fun identityTokenUsesMinecraftFormat() {
        assertEquals("XBL3.0 x=12345;abc.def", XboxProtocol.identityToken("12345", "abc.def"))
    }

    @Test
    fun rpsTicketPrefixesMicrosoftToken() {
        assertEquals("d=token-value", XboxProtocol.rpsTicket("token-value"))
    }

    @Test
    fun knownXErrGivesSpecificMessage() {
        val message = XboxProtocol.errorMessage(XboxProtocol.XERR_NO_XBOX_ACCOUNT)
        assertTrue(message.contains("hồ sơ Xbox"))
    }

    @Test
    fun unknownXErrIncludesCode() {
        assertTrue(XboxProtocol.errorMessage(999L).contains("999"))
    }

    @Test
    fun missingXErrGivesGenericMessage() {
        assertTrue(XboxProtocol.errorMessage(null).isNotBlank())
    }
}
