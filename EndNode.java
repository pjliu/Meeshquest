//package cmsc420.heptatrie;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EndNode<K, V> extends Node<K, V> {
	public EndNode() {
		super(null, NodeType.END);
	}

	public void put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	public V get(K key) {
		throw new UnsupportedOperationException();
	}

	public V remove(K key) {
		throw new UnsupportedOperationException();
	}

	public void addToXmlDoc(Document doc, Element parent) {
		throw new UnsupportedOperationException();
	}

}
