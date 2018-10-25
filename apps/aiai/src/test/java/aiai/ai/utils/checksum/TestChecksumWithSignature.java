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
package aiai.ai.utils.checksum;

import aiai.ai.Globals;
import aiai.apps.commons.utils.SecUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestChecksumWithSignature {

    @Autowired
    public ChecksumWithSignatureService checksumWithSignatureService;

    @Autowired
    public Globals globals;


    @Test
    public void test() throws IOException, GeneralSecurityException {
        File file = new File("config", "private-key.txt");
        String base64 = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        PrivateKey privateKey = SecUtils.getPrivateKey(base64);
        String checksum = "69c33a60e09f00fa3610fb8833bef54487f9c8b99db48b339cd6ed0f192ba5c9";

        String signature = SecUtils.getSignature(checksum, privateKey);

        String forVerifying = checksum + SecUtils.SIGN_DELIMITER + signature;

        ChecksumWithSignatureService.ChecksumWithSignature checksumWithSignature = ChecksumWithSignatureService.parse(forVerifying);

        assertTrue(ChecksumWithSignatureService.isValid(checksumWithSignature.checksum.getBytes(), checksumWithSignature.signature, globals.publicKey));
    }


}