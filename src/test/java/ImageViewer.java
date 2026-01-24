import me.darragh.playingapi.communicator.Communicator;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class ImageViewer {
    private final Communicator communicator;
    private final JLabel albumLabel = new JLabel();
    private final JLabel authorLabel = new JLabel();

    public ImageViewer(Communicator communicator) {
        this.communicator = communicator;
        this.initUI();
        this.startUpdater();
    }

    private void initUI() {
        JFrame frame = new JFrame("Demo Images");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new GridLayout(1, 2));

        this.albumLabel.setHorizontalAlignment(JLabel.CENTER);
        this.authorLabel.setHorizontalAlignment(JLabel.CENTER);

        frame.add(this.albumLabel);
        frame.add(this.authorLabel);

        frame.setSize(600, 300);
        frame.setVisible(true);
    }

    private void startUpdater() {
        new Timer(1000, e -> new Thread(this::updateImages).start()).start();
    }

    private void updateImages() {
        BufferedImage album = this.communicator.isAlbumImageDataAvailable() ? this.communicator.getAlbumImageData() : null;
        BufferedImage author = this.communicator.isArtistImageDataAvailable() ? this.communicator.getArtistImageData() : null;

        SwingUtilities.invokeLater(() -> {
            this.albumLabel.setIcon(album != null ? new ImageIcon(album) : null);
            this.authorLabel.setIcon(author != null ? new ImageIcon(author) : null);
        });
    }
}
