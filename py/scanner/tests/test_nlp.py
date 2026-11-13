import unittest

from scanner.nlp import extrair_fisica

class NlpTest(unittest.TestCase):
    def test_unidades_formulas_conceitos(self):
        f = extrair_fisica("A velocidade é 20 m/s e a massa é 5 kg. F = m * a")
        self.assertIn("20 m/s", f["unidades"])
        self.assertIn("5 kg", f["unidades"])
        self.assertIn("velocidade", f["conceitos"])
        self.assertIn("massa", f["conceitos"])
        self.assertTrue(any(x.startswith("F =") for x in f["formulas"]))


if __name__ == "__main__":
    unittest.main()