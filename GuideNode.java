//package cmsc420.heptatrie;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GuideNode<K, V> extends Node<K, V> {
	protected LinkedList<K> guides;

	protected LinkedList<Node<K, V>> kids;

	protected static final int order = 7;

	public GuideNode(Comparator<K> comparator) {
		super(comparator, NodeType.GUIDE);
		this.guides = new LinkedList<K>();
		this.kids = new LinkedList<Node<K, V>>();
	}

	protected GuideNode(Comparator<K> comparator, List<K> guides,
			List<Node<K, V>> kids) {
		super(comparator, NodeType.GUIDE);
		this.guides = new LinkedList<K>(guides);
		this.kids = new LinkedList<Node<K, V>>(kids);
	}

	public int size() {
		return kids.size();
	}

	public boolean isFull() {
		return kids.size() == order;
	}

	protected boolean isOverFull() {
		return kids.size() > order;
	}
	
	private boolean isUnderHalfFull() {
		return kids.size()*2 < order;
	}	

	public List<K> getGuides() {
		return guides;
	}
	
	public List<Node<K, V>> getKids() {
		return kids;
	}

	public void put(K key, V value) {
		int index = kidIndex(key);

		kids.get(index).put(key, value);
	}
	
	public void updateKeyByIndex(int index, K key) {
		guides.set(index, key);
	}


	public V get(K key) {
		return kids.get(kidIndex(key)).get(key);
	}

	public V remove(K key) {
		return kids.get(kidIndex(key)).remove(key);
	}
	
	private LeafNode getFirstLeaf() {
		Node<K,V> firstChild = getKids().get(0);
		while (firstChild.getType() == NodeType.GUIDE) {
			firstChild = (Node<K,V>)((GuideNode)firstChild).getKids().get(0);
		}
		LeafNode firstLeaf = (LeafNode<K,V>)firstChild;
		return firstLeaf;
	}

	private LeafNode getLastLeaf() {
		Node<K,V> lastChild = getKids().get(size()-1);
		while (lastChild.getType() == NodeType.GUIDE) {
			lastChild = (Node<K,V>)((GuideNode)lastChild).getKids().get(((GuideNode)lastChild).size()-1);
		}
		LeafNode lastLeaf = (LeafNode<K,V>)lastChild;
		return lastLeaf;
	}	
	
	protected void split() {
		int halfGuides = guides.size() / 2;
		int halfKids = halfGuides + 1;
		GuideNode<K, V> left = new GuideNode<K, V>(comparator, guides.subList(
				0, halfGuides), kids.subList(0, halfKids));
		GuideNode<K, V> right = new GuideNode<K, V>(comparator, guides.subList(
				halfGuides + 1, guides.size()), kids.subList(halfKids,
				kids.size()));
		left.setLeft(getLeft());
		left.setRight(right);
		getLeft().setRight(left);
		left.setParent(getParent());
		for (Node<K, V> kid : left.kids) {
			kid.setParent(left);
		}

		right.setLeft(left);
		right.setRight(getRight());
		getRight().setLeft(right);
		right.setParent(getParent());
		for (Node<K, V> kid : right.kids) {
			kid.setParent(right);
		}
		
		parent.insertKid(this, guides.get(halfGuides), left, right);
	}
	
	private void addChild(Node<K,V> node, Node<K,V> child) {
		if (child.getType() == NodeType.LEAF) {
			LeafNode leaf = (LeafNode)child;
			for (int i=0; i<leaf.size(); i++) {
				node.put((K)leaf.getKeys().get(i), (V)leaf.getValues().get(i));
			}
		} else if (child.getType() == NodeType.GUIDE) {
			GuideNode guide = (GuideNode)child;
			for (int i=0; i<guide.size(); i++) {
				addChild(node, (Node<K,V>)guide.getKids().get(i));						
			}
		}		
	}
	protected void merge() {
		if (parent != null) {
			parent.deleteKid(this);
			Node<K,V> prev = null;
			Node<K,V> temp = parent;
			while (temp != null && temp.getType() != NodeType.ROOT) {
				prev = temp;
				temp = temp.getParent();
			}
			if (prev != null) {
				for (int i=0; i<size(); i++) {
					Node<K,V> child = kids.get(i);
					addChild(prev, child);
				}		
			}
		}
	}

	protected void insertKid(Node<K, V> old, K key, Node<K, V> left,
			Node<K, V> right) {
		int ind = kids.indexOf(old);
		if (ind < 0) {
			// when the root splits, the two new nodes are added to the dummy
			// new root
			kids.add(left);
			kids.add(right);
			guides.add(key);
			return;
		}

		kids.set(ind, left);
		kids.add(ind + 1, right);
		guides.add(ind, key);

		if (isOverFull()) {
			split();
		}
	}
	
	public void deleteKid(Node<K,V> obj) {
		int index = kids.indexOf(obj);
		if (index == 0) {		
			guides.remove(index);
			kids.remove(index);
		} else if (index > 0) {
			guides.remove(index-1);
			kids.remove(index);			
		}
		if (isUnderHalfFull()) {
			merge();
		}		
	}		
	
	protected int kidIndex(K key) {
		int i = 0;
		for (K g : guides) {
			if (cmp(key, g) < 0) {
				break;
			}
			++i;
		}
		return i;
	}
		

	public void addToXmlDoc(Document doc, Element parent) {
		Element thisNode = doc.createElement("guide");
		Iterator<K> guideItr = guides.iterator();
		Iterator<Node<K, V>> kidItr = kids.iterator();
		while (guideItr.hasNext()) {
			assert (kidItr.hasNext());
			kidItr.next().addToXmlDoc(doc, thisNode);
			Element key = doc.createElement("key");
			key.setAttribute("value", guideItr.next().toString());
			thisNode.appendChild(key);
		}
		assert (kidItr.hasNext());
		// Remember to add the last child.
		kidItr.next().addToXmlDoc(doc, thisNode);
		parent.appendChild(thisNode);
	}
	
}