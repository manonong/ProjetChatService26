/*
 * Copyright (c) 2026.  Jerome David. Univ. Grenoble Alpes.
 * This file is part of DcissChatService.
 *
 * DcissChatService is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * DcissChatService is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package fr.uga.miashs.dciss.chatservice.common;

import java.io.*;

public class FilePayloadUtil {

    public static byte[] buildFilePayload(String filename, byte[] fileBytes) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeUTF(filename);
        dos.writeInt(fileBytes.length);
        dos.write(fileBytes);
        dos.flush();

        return bos.toByteArray();
    }

    public static DecodedFilePayload readFilePayload(byte[] payload) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(payload);
        DataInputStream dis = new DataInputStream(bis);

        String filename = dis.readUTF();
        int length = dis.readInt();

        byte[] fileBytes = new byte[length];
        dis.readFully(fileBytes);

        return new DecodedFilePayload(filename, fileBytes);
    }

    // remplacer "record" par class
    public static class DecodedFilePayload {
        private String filename;
        private byte[] fileBytes;

        public DecodedFilePayload(String filename, byte[] fileBytes) {
            this.filename = filename;
            this.fileBytes = fileBytes;
        }

        public String getFilename() {
            return filename;
        }

        public byte[] getFileBytes() {
            return fileBytes;
        }
    }
}