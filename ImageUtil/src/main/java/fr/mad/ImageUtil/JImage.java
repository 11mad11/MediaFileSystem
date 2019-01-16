package fr.mad.ImageUtil;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;

public class JImage extends JComponent {
	private static final long serialVersionUID = 1316135741421161388L;
	private Image image = null;
	private SizeMode sizeMode = SizeMode.CENTER;
	private Dimension size;
	
	public JImage(Image image) {
		this.image = image;
		if (image != null)
			this.setPreferredSize(size = new Dimension(image.getWidth(null), image.getHeight(null)));
		this.setOpaque(false);
		this.setSizeMode(sizeMode);
	}
	
	@Override
	public void paintComponent(Graphics g) {
		if (image != null) {
			switch (getSizeMode()) {
				case NORMAL:
					g.drawImage(image, 0, 0, size.width, size.height, null);
					break;
				case ZOOM:
					int newWidth = Math.max(size.width, this.getWidth());
					int newHeight = Math.max(size.height, this.getHeight());
					g.drawImage(image, 0, 0, newWidth, newHeight, null);
					break;
				case STRETCH:
					g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);
					break;
				case CENTER:
					float scale = Math.max(size.width / (float) this.getWidth(), size.height / (float) this.getHeight());
					newWidth = (int) (size.width / scale);
					newHeight = (int) (size.height / scale);
					g.drawImage(image, (this.getWidth() - newWidth) / 2, (this.getHeight() - newHeight) / 2, newWidth, newHeight, null);
					//g.drawImage(image, (int) (this.getWidth() / 2) - (int) (size.width / 2), (int) (this.getHeight() / 2) - (int) (size.height / 2), size.width, size.height, null);
					break;
				default:
					g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);
			}
		}
	}
	
	public Image getImage() {
		return image;
	}
	
	public void setImage(Image image) {
		this.image = image;
		if (image != null)
			size = new Dimension(image.getWidth(null), image.getHeight(null));
	}
	
	public SizeMode getSizeMode() {
		return sizeMode;
	}
	
	public void setSizeMode(SizeMode sizeMode) {
		this.sizeMode = sizeMode;
	}
	
	public enum SizeMode {
		NORMAL,
		STRETCH,
		CENTER,
		ZOOM
	}
}