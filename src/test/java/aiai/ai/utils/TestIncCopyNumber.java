/*
 AiAi, Copyright (C) 2017 - 2018, Serge Maslyukov

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */
package aiai.ai.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestIncCopyNumber {

    @Test
    public void testIncNumber() {
        assertEquals("Copy #2, aaa", StrUtils.incCopyNumber("aaa"));
        assertEquals("Copy #3, aaa", StrUtils.incCopyNumber("Copy #2, aaa"));
        assertEquals("Copy #2, aaa", StrUtils.incCopyNumber("Copy #aa2, aaa"));
    }
}
