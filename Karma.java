class Karma {
	private int karma = 0;
	private boolean credit = false;

	public Karma() {
	}

	public Karma(int i, boolean b) {
		karma = i;
		credit = b;
	}

	public void giveCredit() {
		credit = true;
	}

	public void increment() {
		if(credit) { karma++; credit = false; }
	}

	public void decrement() {
		if(credit) { karma--; credit = false; }
	}

	public int getKarma() { return karma; }
}
