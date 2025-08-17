package projectCG;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class LofiTaoBinVendingMachine extends JPanel implements ActionListener, MouseListener {
    private Timer animationTimer;
    private float time = 0;
    private List<FallingItem> fallingItems;
    private List<Particle> particles;
    private Random random;
    private int floorY;
    private boolean animationComplete = false;

    // Animation states
    private boolean machineDropped = false;
    private float machineY = -500;
    private float machineTargetY;
    private float machineVelocityY = 0;
    private boolean machineOnGround = false;

    // Lofi color palette
    private final Color[] lofiColors = {
            new Color(255, 154, 158), // Pink
            new Color(255, 206, 239), // Light Pink
            new Color(163, 196, 243), // Light Blue
            new Color(144, 219, 244), // Sky Blue
            new Color(255, 183, 197), // Rose
            new Color(207, 186, 240), // Lavender
            new Color(255, 218, 193), // Peach
            new Color(181, 234, 215), // Mint
            new Color(255, 223, 186), // Cream
            new Color(186, 225, 255) // Baby Blue
    };

    private static class FallingItem {
        float x, y, vx, vy, rotation, rotationSpeed;
        Color color;
        boolean isVisible;
        float alpha = 1.0f;
        float size = 20f;
        boolean onGround = false;
        float bounciness = 0.6f;
        float friction = 0.8f;

        public FallingItem(float x, float y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = (float) (Math.random() - 0.5) * 4;
            this.vy = 2;
            this.rotation = 0;
            this.rotationSpeed = (float) (Math.random() - 0.5) * 0.2f;
            this.isVisible = true;
        }

        public void update(int floorY, int frameWidth) {
            if (isVisible) {
                x += vx;
                y += vy;

                if (!onGround) {
                    vy += 0.8f;
                }

                rotation += rotationSpeed;

                if (y + size / 2 >= floorY && vy > 0) {
                    y = floorY - size / 2;
                    vy *= -bounciness;
                    vx *= friction;
                    rotationSpeed *= 0.9f;

                    if (Math.abs(vy) < 1.5f) {
                        vy = 0;
                        onGround = true;
                    }
                }

                if (x - size / 2 <= 0 && vx < 0) {
                    x = size / 2;
                    vx *= -bounciness;
                } else if (x + size / 2 >= frameWidth && vx > 0) {
                    x = frameWidth - size / 2;
                    vx *= -bounciness;
                }

                if (onGround) {
                    vx *= 0.95f;
                    if (Math.abs(vx) < 0.1f) {
                        vx = 0;
                    }
                }
            }
        }
    }

    private static class Particle {
        float x, y, vx, vy;
        Color color;
        float life, maxLife;
        float size;

        public Particle(float x, float y) {
            this.x = x;
            this.y = y;
            this.vx = (float) (Math.random() - 0.5) * 2;
            this.vy = (float) (Math.random() - 0.5) * 2 - 1;
            this.maxLife = this.life = (float) Math.random() * 2 + 1;
            this.size = (float) Math.random() * 3 + 1;
            this.color = new Color(255, 255, 255, 100);
        }

        public void update() {
            x += vx;
            y += vy;
            life -= 0.02f;
        }

        public boolean isAlive() {
            return life > 0;
        }

        public float getAlpha() {
            return life / maxLife;
        }
    }

    public LofiTaoBinVendingMachine() {
        setPreferredSize(new Dimension(600, 600));
        setBackground(new Color(255, 240, 245));

        fallingItems = new ArrayList<>();
        particles = new ArrayList<>();
        random = new Random();

        floorY = 550;
        machineTargetY = floorY - 420 + 10;

        // Start animation immediately
        machineDropped = true;
        machineVelocityY = 0;

        animationTimer = new Timer(16, this); // ~60 FPS
        animationTimer.start();

        addMouseListener(this);

        // Add initial particles
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(random.nextFloat() * 600, random.nextFloat() * 600));
        }

        // Stop animation after 2 seconds (120 frames at 60 FPS)
        Timer stopTimer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animationComplete = true;
                animationTimer.stop();
                ((Timer) e.getSource()).stop();
            }
        });
        stopTimer.setRepeats(false);
        stopTimer.start();
    }

    private void dispenseItems() {
        // Dispense 5 items at once when machine lands
        Color itemColor = lofiColors[random.nextInt(lofiColors.length)];
        int machineX = (getWidth() - 200) / 2;
        int machineY = (int) machineTargetY;
        int areaX = machineX + 40;
        int areaWidth = 120;
        int areaY = machineY + 340;

        for (int i = 0; i < 5; i++) {
            float randomX = areaX + random.nextFloat() * areaWidth;
            float randomY = areaY;
            FallingItem newItem = new FallingItem(randomX, randomY, itemColor);
            newItem.vy = 1.5f + random.nextFloat() * 0.5f;
            newItem.vx = (float) (Math.random() - 0.5) * 8f;
            newItem.size = 15f + random.nextFloat() * 5f;
            fallingItems.add(newItem);
        }

        // Add dispensing particles
        for (int i = 0; i < 10; i++) {
            float particleX = areaX + random.nextFloat() * areaWidth;
            float particleY = areaY;
            Particle sparkle = new Particle(particleX, particleY);
            sparkle.vx = (float) (Math.random() - 0.5) * 5;
            sparkle.vy = (float) (Math.random() - 0.5) * 4 - 1;
            sparkle.color = new Color(itemColor.getRed(), itemColor.getGreen(), itemColor.getBlue(), 180);
            particles.add(sparkle);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawLofiSkyBackground(g2d);
        drawLofiGreenGlass(g2d, floorY - 50);
        drawParticles(g2d);

        // Draw vertical Japanese text
        drawAnimatedVerticalJapaneseText(g2d, "立方体の自動販売機", 50, 50, 20);

        // Calculate machine position
        int cubeWidth = 200;
        int cubeHeight = 420;
        int machineX = (getWidth() - cubeWidth) / 2;
        int currentMachineY = (int) machineY;

        // Draw shadow and machine
        drawLofiCubeShadow(g2d, machineX + 20, currentMachineY - 20, cubeWidth, cubeHeight);
        drawLofiCubicVendingMachine(g2d, machineX, currentMachineY, cubeWidth, cubeHeight);

        // Draw falling items
        if (machineOnGround) {
            drawFallingItems(g2d);
        }

        // Show completion message
        if (animationComplete) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRoundRect(getWidth() / 2 - 100, getHeight() / 2 - 30, 200, 60, 20, 20);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Animation Complete!", getWidth() / 2 - 80, getHeight() / 2 + 5);
        }
    }

    private void drawLofiSkyBackground(Graphics2D g2d) {
        float t = time * 0.01f;
        Color topColor = new Color(
                (int) (135 + 50 * Math.sin(t)),
                (int) (206 + 30 * Math.cos(t * 0.8f)),
                (int) (250 + 5 * Math.sin(t * 1.2f)));
        Color bottomColor = new Color(
                (int) (255 - 20 * Math.sin(t * 0.5f)),
                (int) (240 + 15 * Math.cos(t * 0.7f)),
                (int) (245 + 10 * Math.sin(t * 0.9f)));

        GradientPaint skyGradient = new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor);
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        drawAnimatedClouds(g2d);
    }

    private void drawAnimatedClouds(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 120));
        float t = time * 0.005f;

        int cloud1X = (int) (100 + 20 * Math.sin(t));
        int cloud1Y = (int) (80 + 10 * Math.cos(t * 1.3f));
        drawLofiCloud(g2d, cloud1X, cloud1Y, 80);

        int cloud2X = (int) (480 + 15 * Math.sin(t * 0.8f));
        int cloud2Y = (int) (60 + 8 * Math.cos(t * 1.1f));
        drawLofiCloud(g2d, cloud2X, cloud2Y, 100);
    }

    private void drawLofiCloud(Graphics2D g2d, int x, int y, int size) {
        g2d.fillOval(x, y, size, size / 2);
        g2d.fillOval(x + size / 3, y - size / 6, size / 2, size / 3);
        g2d.fillOval(x - size / 8, y + size / 10, size / 3, size / 4);
        g2d.fillOval(x + size / 2, y + size / 10, size / 3, size / 4);
    }

    private void drawLofiGreenGlass(Graphics2D g2d, int floorY) {
        float t = time * 0.008f;
        Color glass1 = new Color(
                (int) (100 + 30 * Math.sin(t)),
                (int) (200 + 40 * Math.cos(t * 0.7f)),
                (int) (150 + 20 * Math.sin(t * 1.1f)),
                120);
        Color glass2 = new Color(
                (int) (50 + 40 * Math.sin(t * 0.5f)),
                (int) (180 + 50 * Math.cos(t * 0.9f)),
                (int) (120 + 30 * Math.sin(t * 0.8f)),
                150);

        GradientPaint glassGradient = new GradientPaint(0, floorY, glass1, 0, getHeight(), glass2);
        g2d.setPaint(glassGradient);
        g2d.fillRect(0, floorY, getWidth(), getHeight() - floorY);
    }

    private void drawLofiCubeShadow(Graphics2D g2d, int x, int y, int width, int height) {
        RadialGradientPaint shadowGradient = new RadialGradientPaint(
                x + width / 2 + 20, y + height + 20,
                width * 0.8f,
                new float[] { 0f, 1f },
                new Color[] { new Color(0, 0, 0, 60), new Color(0, 0, 0, 0) });
        g2d.setPaint(shadowGradient);
        g2d.fillOval(x - 20, y + height - 10, width + 80, 60);
    }

    private void drawLofiCubicVendingMachine(Graphics2D g2d, int x, int y, int width, int height) {
        drawLofiCubeFrontFace(g2d, x, y, width, height);
        drawLofiCubeRightFace(g2d, x, y, width, height);
        drawLofiCubeTopFace(g2d, x, y, width, height);
        drawLofiCubeEdges(g2d, x, y, width, height);
    }

    private void drawLofiCubeFrontFace(Graphics2D g2d, int x, int y, int width, int height) {
        // Front face gradient
        Color front1 = new Color(250, 245, 250);
        Color front2 = new Color(220, 215, 220);
        GradientPaint frontGradient = new GradientPaint(x, y, front1, x + width, y + height, front2);
        g2d.setPaint(frontGradient);
        g2d.fillRoundRect(x, y, width, height, 20, 20);

        // Header
        Color headerColor = lofiColors[0];
        g2d.setColor(headerColor);
        g2d.fillRoundRect(x + 15, y + 15, width - 30, 50, 15, 15);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("TAO BIN", x + 30, y + 38);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("CUBIC BEVERAGE LOFI", x + 30, y + 52);

        // Screen
        g2d.setColor(new Color(20, 20, 30));
        g2d.fillRoundRect(x + 20, y + 80, width - 40, height - 180, 15, 15);

        g2d.setColor(new Color(30, 30, 40));
        g2d.fillRoundRect(x + 25, y + 85, width - 50, height - 190, 10, 10);

        drawLofiCubicBeverageGrid(g2d, x + 30, y + 90, width - 60, height - 200);

        // Payment area
        g2d.setColor(new Color(240, 230, 255));
        g2d.fillRoundRect(x + 20, y + height - 90, width - 40, 65, 10, 10);

        // Dispensing area
        drawLofiDispensingArea(g2d, x, y - 32, width, height);
    }

    private void drawLofiDispensingArea(Graphics2D g2d, int x, int y, int width, int height) {
        int areaX = x + 40;
        int areaY = y + height - 50;
        int areaWidth = width - 80;
        int areaHeight = 50;

        GradientPaint dispensingGradient = new GradientPaint(
                areaX, areaY, new Color(60, 60, 80),
                areaX, areaY + areaHeight, new Color(40, 40, 60));
        g2d.setPaint(dispensingGradient);
        g2d.fillRoundRect(areaX, areaY, areaWidth, areaHeight, 15, 15);

        g2d.setColor(new Color(100, 150, 200, 80));
        g2d.fillRoundRect(areaX + 5, areaY + 5, areaWidth - 10, areaHeight - 10, 10, 10);
    }

    private void drawLofiCubeRightFace(Graphics2D g2d, int x, int y, int width, int height) {
        int depth = 35;
        Polygon rightFace = new Polygon();
        rightFace.addPoint(x + width, y);
        rightFace.addPoint(x + width + depth, y - depth);
        rightFace.addPoint(x + width + depth, y + height - depth);
        rightFace.addPoint(x + width, y + height);

        Color side1 = lofiColors[1];
        Color side2 = lofiColors[1].darker();
        GradientPaint sideGradient = new GradientPaint(x + width, y, side1, x + width + depth, y + height - depth,
                side2);
        g2d.setPaint(sideGradient);
        g2d.fill(rightFace);
    }

    private void drawLofiCubeTopFace(Graphics2D g2d, int x, int y, int width, int height) {
        int depth = 35;
        Polygon topFace = new Polygon();
        topFace.addPoint(x, y);
        topFace.addPoint(x + depth, y - depth);
        topFace.addPoint(x + width + depth, y - depth);
        topFace.addPoint(x + width, y);

        Color top1 = lofiColors[4];
        Color top2 = lofiColors[4].darker();
        GradientPaint topGradient = new GradientPaint(x, y, top1, x + width + depth, y - depth, top2);
        g2d.setPaint(topGradient);
        g2d.fill(topFace);
    }

    private void drawLofiCubeEdges(Graphics2D g2d, int x, int y, int width, int height) {
        int depth = 35;
        g2d.setColor(new Color(lofiColors[0].getRed(), lofiColors[0].getGreen(), lofiColors[0].getBlue(), 200));
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2d.drawRoundRect(x, y, width, height, 20, 20);
        g2d.drawLine(x, y, x + depth, y - depth);
        g2d.drawLine(x + width, y, x + width + depth, y - depth);
        g2d.drawLine(x + width, y + height, x + width + depth, y + height - depth);
        g2d.drawLine(x + depth, y - depth, x + width + depth, y - depth);
        g2d.drawLine(x + width + depth, y - depth, x + width + depth, y + height - depth);
    }

    private void drawLofiCubicBeverageGrid(Graphics2D g2d, int x, int y, int width, int height) {
        int rows = 6;
        int cols = 3;
        int cellWidth = width / cols;
        int cellHeight = height / rows;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int cellX = x + col * cellWidth;
                int cellY = y + row * cellHeight;
                drawLofiSmallCube(g2d, cellX + 3, cellY + 3, cellWidth - 6, cellHeight - 6, row, col);
            }
        }
    }

    private void drawLofiSmallCube(Graphics2D g2d, int x, int y, int width, int height, int row, int col) {
        int colorIndex = (row * 3 + col) % lofiColors.length;
        Color baseColor = lofiColors[colorIndex];

        GradientPaint cubeGradient = new GradientPaint(x, y, baseColor, x + width, y + height, baseColor.darker());
        g2d.setPaint(cubeGradient);
        g2d.fillRoundRect(x, y, width - 10, height - 10, 5, 5);

        // Right face
        g2d.setColor(new Color(
                Math.max(0, baseColor.getRed() - 40),
                Math.max(0, baseColor.getGreen() - 40),
                Math.max(0, baseColor.getBlue() - 40)));
        Polygon rightFace = new Polygon();
        rightFace.addPoint(x + width - 10, y);
        rightFace.addPoint(x + width - 2, y - 8);
        rightFace.addPoint(x + width - 2, y + height - 18);
        rightFace.addPoint(x + width - 10, y + height - 10);
        g2d.fill(rightFace);

        // Top face
        g2d.setColor(new Color(
                Math.min(255, baseColor.getRed() + 30),
                Math.min(255, baseColor.getGreen() + 30),
                Math.min(255, baseColor.getBlue() + 30)));
        Polygon topFace = new Polygon();
        topFace.addPoint(x, y);
        topFace.addPoint(x + 8, y - 8);
        topFace.addPoint(x + width - 2, y - 8);
        topFace.addPoint(x + width - 10, y);
        g2d.fill(topFace);
    }

    private void drawAnimatedVerticalJapaneseText(Graphics2D g2d, String text, int x, int y, int lineSpacing) {
        float t = time * 0.01f;
        Color textColor = new Color(
                (int) (139 + 50 * Math.sin(t)),
                (int) (69 + 30 * Math.cos(t * 0.8f)),
                (int) (19 + 20 * Math.sin(t * 1.2f)),
                (int) (200 + 50 * Math.sin(t * 0.5f)));

        g2d.setColor(textColor);
        g2d.setFont(new Font("Dialog", Font.BOLD, 18));

        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            if (!ch.equals("\n")) {
                int charX = (int) (x + 5 * Math.sin(time * 0.02f + i * 0.3f));
                int charY = (int) (y + i * lineSpacing + 3 * Math.cos(time * 0.015f + i * 0.2f));
                g2d.drawString(ch, charX, charY);
            }
        }
    }

    private void drawParticles(Graphics2D g2d) {
        for (Particle particle : particles) {
            if (particle.isAlive()) {
                int alpha = (int) (255 * particle.getAlpha());
                g2d.setColor(new Color(255, 255, 255, alpha));
                g2d.fillOval((int) particle.x, (int) particle.y, (int) particle.size, (int) particle.size);
            }
        }
    }

    private void drawFallingItems(Graphics2D g2d) {
        for (FallingItem item : fallingItems) {
            if (item.isVisible) {
                g2d.setColor(new Color(
                        item.color.getRed(),
                        item.color.getGreen(),
                        item.color.getBlue(),
                        (int) (255 * item.alpha)));

                AffineTransform oldTransform = g2d.getTransform();
                g2d.translate(item.x, item.y);
                g2d.rotate(item.rotation);

                int halfSize = (int) (item.size / 2);
                g2d.fillRoundRect(-halfSize, -halfSize, (int) item.size, (int) item.size, 5, 5);

                g2d.setTransform(oldTransform);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (animationComplete)
            return;

        time++;

        // Machine dropping animation
        if (machineDropped && !machineOnGround) {
            machineVelocityY += 1.2f; // Faster gravity for quicker animation
            machineY += machineVelocityY;

            if (machineY >= machineTargetY) {
                machineY = machineTargetY;
                machineVelocityY *= -0.3f;

                if (Math.abs(machineVelocityY) < 2.0f) {
                    machineOnGround = true;
                    machineVelocityY = 0;
                    dispenseItems(); // Dispense items immediately when landing

                    // Add landing particles
                    for (int i = 0; i < 15; i++) {
                        int machineX = (getWidth() - 200) / 2;
                        Particle landingEffect = new Particle(
                                machineX + 100 + (random.nextFloat() - 0.5f) * 200,
                                (float) machineY + 420);
                        landingEffect.vx = (float) (Math.random() - 0.5) * 8;
                        landingEffect.vy = (float) (Math.random() * -5 - 2);
                        landingEffect.color = lofiColors[random.nextInt(lofiColors.length)];
                        particles.add(landingEffect);
                    }
                }
            }
        }

        // Update falling items
        if (machineOnGround) {
            for (FallingItem item : fallingItems) {
                item.update(floorY, getWidth());
            }
        }

        // Update particles
        particles.removeIf(particle -> !particle.isAlive());
        for (Particle particle : particles) {
            particle.update();
        }

        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Do nothing - animation runs automatically
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("立方体の自動販売機 - Lofi Cubic Vending Machine");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new LofiTaoBinVendingMachine());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.setResizable(false);
        });
    }
}