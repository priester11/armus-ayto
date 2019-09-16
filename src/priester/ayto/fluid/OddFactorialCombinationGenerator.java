package priester.ayto.fluid;

public class OddFactorialCombinationGenerator {
	private OddFactorialCombinationListener listener;

	public OddFactorialCombinationGenerator(OddFactorialCombinationListener listener) {
		this.listener = listener;
	}

	public void generate(int n) throws IllegalArgumentException {
		int[] array = new int[n];
		for (int i = 0; i < n; i++)
			array[i] = i;

		if (n < 2 || n % 2 != 0)
			throw new IllegalArgumentException("The length of the array must be even.");

		listener.onNextCombination(array);

		if (n != 2) {
			for (int lvl = 1; lvl < n / 2; lvl++)
				executeLevel(array, lvl);
		}

		listener.onComplete();
	}

	private void executeLevel(int[] array, int lvl) {
		int i = lvl * 2;
		for (int j = i - 1; j >= 0; j--) {
			swap(array, i, j);
			listener.onNextCombination(array);
			for (int temp = 1; temp < lvl; temp++)
				executeLevel(array, temp);
			swap(array, i, j);
		}
	}

	private void swap(int[] array, int i, int j) {
		int temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

	public interface OddFactorialCombinationListener {
		public void onNextCombination(int[] array);

		public void onComplete();
	}
}