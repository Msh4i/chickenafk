package dev.chickenafk;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Development only: a minimal RCON client, so the bot test can play the server's part (titles, items, kicks). */
final class Rcon implements AutoCloseable {
	private final Socket socket;
	private final DataInputStream in;
	private final OutputStream out;
	private int nextId = 1;

	Rcon(String host, int port, String password) throws IOException {
		socket = new Socket(host, port);
		socket.setSoTimeout(5000);
		in = new DataInputStream(socket.getInputStream());
		out = socket.getOutputStream();
		if (request(3, password) == -1) {
			throw new IOException("RCON login refused");
		}
	}

	/** Runs a server command and returns its reply. */
	String command(String command) throws IOException {
		write(2, command);
		return read().body;
	}

	private int request(int type, String body) throws IOException {
		write(type, body);
		return read().id;
	}

	private record Reply(int id, String body) {
	}

	private void write(int type, String body) throws IOException {
		byte[] payload = body.getBytes(StandardCharsets.UTF_8);
		ByteBuffer b = ByteBuffer.allocate(14 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(10 + payload.length).putInt(nextId++).putInt(type).put(payload).put((byte) 0).put((byte) 0);
		out.write(b.array());
		out.flush();
	}

	private Reply read() throws IOException {
		byte[] head = new byte[4];
		in.readFully(head);
		int length = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN).getInt();
		byte[] rest = new byte[length];
		in.readFully(rest);
		ByteBuffer b = ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN);
		int id = b.getInt();
		b.getInt();
		String body = new String(rest, 8, Math.max(0, length - 10), StandardCharsets.UTF_8);
		return new Reply(id, body);
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}
}
