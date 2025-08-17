package projectCG;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class SideView extends JPanel implements ActionListener {
    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 600;

    private Timer timer;
    private VendingMachine vendingMachine;
    private List<Cloud> clouds;
    private List<RainDrop> rainDrops;
    private List<PortalParticle> portalParticles = new ArrayList<>();
    private List<CrashParticle> crashParticles = new ArrayList<>();
    private Random random;
    private float time = 0;
    private boolean animationComplete = false;
    private int completionTimer = 0;

    // Static frame reference for transitions
    private static JFrame frame;

    // Double buffering for smoother rendering
    private BufferedImage backBuffer;
    private Graphics2D backBufferGraphics;

    // Lofi color palette
    private final Color SKY_COLOR = new Color(176, 196, 222, 200);
    private final Color GROUND_COLOR = new Color(101, 134, 118);
    private final Color VENDING_BASE = new Color(218, 165, 152);
    private final Color VENDING_ACCENT = new Color(244, 208, 186);
    private final Color CLOUD_COLOR = new Color(255, 255, 255, 120);
    private final Color RAIN_COLOR = new Color(173, 216, 230, 80);

    public SideView() {
        this.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        this.setBackground(GROUND_COLOR);
        this.setFocusable(true);
        this.setDoubleBuffered(true);
        System.out.println("SideView initialized");

        // Initialize objects
        clouds = new ArrayList<>();
        rainDrops = new ArrayList<>();
        portalParticles = new CopyOnWriteArrayList<>();
        crashParticles = new CopyOnWriteArrayList<>();
        random = new Random();

        // Create single vending machine at center top
        vendingMachine = new VendingMachine(WINDOW_WIDTH / 2, -100);

        // Create initial cloud layer
        for (int i = 0; i < 8; i++) {
            clouds.add(new Cloud(random.nextInt(WINDOW_WIDTH),
                    random.nextInt(120) + 30,
                    30 + random.nextFloat() * 40));
        }

        // Create gentle rain drops
        for (int i = 0; i < 60; i++) {
            rainDrops.add(new RainDrop(
                    random.nextFloat() * WINDOW_WIDTH,
                    random.nextFloat() * WINDOW_HEIGHT,
                    1 + random.nextFloat() * 2));
        }

        // Start animation
        timer = new Timer(16, this); // ~60 FPS
    }

    // Static method to set frame reference
    public static void setFrame(JFrame f) {
        frame = f;
    }

    public void startAnimation() {
        if (timer != null && !timer.isRunning()) {
            timer.start();
        }
    }

    public void stopAnimation() {
        if (timer != null) {
            timer.stop();
        }
        cleanupResources();
    }

    private void cleanupResources() {
        try {
            if (backBufferGraphics != null) {
                backBufferGraphics.dispose();
                backBufferGraphics = null;
            }
            if (backBuffer != null) {
                backBuffer.flush();
                backBuffer = null;
            }
            // Clear particle collections
            if (portalParticles != null)
                portalParticles.clear();
            if (crashParticles != null)
                crashParticles.clear();
            if (clouds != null)
                clouds.clear();
            if (rainDrops != null)
                rainDrops.clear();
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        createBackBuffer();
        startAnimation();
    }

    @Override
    public void removeNotify() {
        stopAnimation();
        super.removeNotify();
    }

    private void createBackBuffer() {
        try {
            if (getWidth() > 0 && getHeight() > 0) {
                // Clean up old buffer
                if (backBufferGraphics != null) {
                    backBufferGraphics.dispose();
                }

                backBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                backBufferGraphics = backBuffer.createGraphics();
                backBufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                backBufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            }
        } catch (Exception e) {
            System.err.println("Error creating back buffer: " + e.getMessage());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (g == null)
            return;

        try {
            super.paintComponent(g);

            // Create back buffer if needed
            if (backBuffer == null || backBuffer.getWidth() != getWidth() || backBuffer.getHeight() != getHeight()) {
                createBackBuffer();
            }

            if (backBufferGraphics == null)
                return;

            // Clear back buffer
            backBufferGraphics.setColor(getBackground());
            backBufferGraphics.fillRect(0, 0, getWidth(), getHeight());

            // Enable anti-aliasing for smooth graphics
            backBufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            backBufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Draw animated gradient sky
            drawLofiSky(backBufferGraphics);

            // Draw rain
            for (RainDrop rain : rainDrops) {
                rain.draw(backBufferGraphics);
            }

            // Draw clouds (background layer)
            for (Cloud cloud : clouds) {
                if (!cloud.dispersing) {
                    cloud.draw(backBufferGraphics);
                }
            }

            // Draw subtle ground pattern
            drawGround(backBufferGraphics);

            // Draw vending machine shadow first (on ground)
            if (vendingMachine != null) {
                vendingMachine.drawShadow(backBufferGraphics);
            }

            // Draw vending machine
            if (vendingMachine != null) {
                vendingMachine.draw(backBufferGraphics);
            }

            // Draw dispersing clouds (foreground layer)
            for (Cloud cloud : clouds) {
                if (cloud.dispersing) {
                    cloud.draw(backBufferGraphics);
                }
            }

            // Draw portal particles
            for (PortalParticle particle : portalParticles) {
                particle.draw(backBufferGraphics);
            }

            // Draw crash particles
            for (CrashParticle particle : crashParticles) {
                particle.draw(backBufferGraphics);
            }

            // Draw floating particles for atmosphere
            drawParticles(backBufferGraphics);

            // Draw title
            drawTitle(backBufferGraphics);

            // Draw back buffer to screen
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(backBuffer, 0, 0, null);

            // ลบส่วนนี้ออก - ไม่วาด completion message อีกต่อไป
            // if (animationComplete) {
            // drawCompletionMessage(g2d);
            // }

        } catch (Exception e) {
            System.err.println("Error in paintComponent: " + e.getMessage());
        }
    }

    private void drawLofiSky(Graphics2D g2d) {
        // Create animated gradient sky
        float offset = (float) Math.sin(time * 0.008) * 30;

        GradientPaint skyGradient = new GradientPaint(
                0, 0, new Color(176, 196, 222, 200),
                0, WINDOW_HEIGHT / 2 + offset, new Color(244, 208, 186, 120));

        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    private void drawGround(Graphics2D g2d) {
        g2d.setColor(GROUND_COLOR);
        g2d.fillRect(0, WINDOW_HEIGHT - 120, WINDOW_WIDTH, 120);

        // Add subtle texture lines
        g2d.setColor(new Color(85, 118, 102));
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int i = 0; i < WINDOW_WIDTH; i += 60) {
            int y = WINDOW_HEIGHT - 120 + (int) (Math.sin(i * 0.05 + time * 0.02) * 4);
            g2d.drawLine(i, y, i + 30, y);
        }

        // Add small grass details
        g2d.setColor(new Color(118, 151, 135));
        for (int i = 0; i < WINDOW_WIDTH; i += 40) {
            int x = i + (int) (Math.sin(time * 0.01 + i) * 3);
            int y = WINDOW_HEIGHT - 120 + (int) (Math.sin(i * 0.1) * 2);
            g2d.drawLine(x, y, x, y - 5);
            g2d.drawLine(x + 5, y, x + 5, y - 3);
        }
    }

    private void drawParticles(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 60));
        for (int i = 0; i < 15; i++) {
            float x = (float) ((Math.sin(time * 0.01 + i) * 200 + WINDOW_WIDTH / 2) % WINDOW_WIDTH);
            float y = (float) ((Math.cos(time * 0.015 + i * 0.7) * 150 + WINDOW_HEIGHT / 2) % WINDOW_HEIGHT);
            float size = 1.5f + (float) Math.sin(time * 0.02 + i) * 1f;

            g2d.fill(new Ellipse2D.Float(x, y, size, size));
        }
    }

    private void drawTitle(Graphics2D g2d) {
        // Draw title with lofi styling
        g2d.setColor(new Color(245, 245, 245, 180));
        g2d.fillRoundRect(WINDOW_WIDTH - 250, 30, 220, 60, 20, 20);

        g2d.setColor(new Color(101, 134, 118));
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Side View Fall", WINDOW_WIDTH - 240, 55);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("~ single drop ~", WINDOW_WIDTH - 235, 75);
    }

    // ลบ method drawCompletionMessage ออกทั้งหมด

    @Override
    public void actionPerformed(ActionEvent e) {
        time++;

        // Update rain
        for (RainDrop rain : rainDrops) {
            rain.update();
        }

        // Update clouds
        Iterator<Cloud> cloudIterator = clouds.iterator();
        while (cloudIterator.hasNext()) {
            Cloud cloud = cloudIterator.next();
            cloud.update();
            if (cloud.shouldRemove()) {
                cloudIterator.remove();
            }
        }

        // Update vending machine
        if (vendingMachine != null && !animationComplete) {
            vendingMachine.update();

            // Check if animation is complete
            if (vendingMachine.onGround && vendingMachine.timeOnGround > 0) { // 0 seconds
                animationComplete = true;
                completionTimer = 0;
                System.out.println("SideView animation complete, starting transition timer...");
            }
        }

        // Handle completion timer - Auto transition to next scene
        if (animationComplete) {
            completionTimer++;
            if (completionTimer > 0) { // 0 seconds after completion
                transitionToNextScene();
            }
        }

        // Update portal particles
        List<PortalParticle> toRemove = new ArrayList<>();
        for (PortalParticle particle : portalParticles) {
            particle.update();
            if (particle.isDead()) {
                toRemove.add(particle);
            }
        }
        portalParticles.removeAll(toRemove);

        // Update crash particles
        List<CrashParticle> toRemove1 = new ArrayList<>();
        for (CrashParticle particle : crashParticles) {
            particle.update();
        }
        crashParticles.removeIf(CrashParticle::isDead);

        // Add new background clouds occasionally
        if (random.nextInt(600) == 0 && clouds.size() < 10) {
            clouds.add(new Cloud(random.nextInt(WINDOW_WIDTH),
                    random.nextInt(100) + 40,
                    30 + random.nextFloat() * 40));
        }

        repaint();
    }

    private void transitionToNextScene() {
        try {
            if (frame != null) {
                System.out.println("Transitioning to next scene...");

                // Stop current animation
                stopAnimation();

                LofiTaoBinVendingMachine nextScene = new LofiTaoBinVendingMachine();
                LofiTaoBinVendingMachine.setFrame(frame);
                frame.setContentPane(nextScene);

                // For now, just print a message
                System.out.println("Ready to transition to next scene - please implement your next scene class!");

                frame.revalidate();
                frame.repaint();

                nextScene.startAnimation();

                System.out.println("Transition ready!");
            } else {
                System.err.println("Frame reference is null - cannot transition");
            }
        } catch (Exception ex) {
            System.err.println("Could not transition to next scene: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Portal particle for spawn effects
    class PortalParticle {
        float x, y, vx, vy, life, maxLife, size;
        Color color;

        public PortalParticle(float x, float y) {
            this.x = x;
            this.y = y;
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float speed = 1 + random.nextFloat() * 3;
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
            this.maxLife = life = 30 + random.nextFloat() * 60;
            this.size = 2 + random.nextFloat() * 4;
            this.color = new Color(244, 208, 186, 200);
        }

        public void update() {
            x += vx;
            y += vy;
            life--;
            vx *= 0.98f;
            vy *= 0.98f;
        }

        public boolean isDead() {
            return life <= 0;
        }

        public void draw(Graphics2D g2d) {
            float alpha = life / maxLife;
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int) (alpha * color.getAlpha())));
            g2d.fill(new Ellipse2D.Float(x - size / 2, y - size / 2, size, size));
        }
    }

    // Crash particle for impact effects
    class CrashParticle {
        float x, y, vx, vy, life, maxLife, size;
        Color color;

        public CrashParticle(float x, float y) {
            this.x = x;
            this.y = y;
            this.vx = (random.nextFloat() - 0.5f) * 8;
            this.vy = -random.nextFloat() * 6;
            this.maxLife = life = 20 + random.nextFloat() * 40;
            this.size = 1 + random.nextFloat() * 3;
            this.color = new Color(218, 165, 152, 180);
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.2f; // gravity
            life--;
            vx *= 0.99f;
        }

        public boolean isDead() {
            return life <= 0;
        }

        public void draw(Graphics2D g2d) {
            float alpha = life / maxLife;
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int) (alpha * color.getAlpha())));
            g2d.fill(new Ellipse2D.Float(x - size / 2, y - size / 2, size, size));
        }
    }

    // Rain drop class
    class RainDrop {
        float x, y, speed;
        float length;
        float opacity;

        public RainDrop(float x, float y, float speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.length = 8 + random.nextFloat() * 12;
            this.opacity = 20 + random.nextFloat() * 40;
        }

        public void update() {
            y += speed;
            x += Math.sin(time * 0.008 + x * 0.008) * 0.2; // slight wind effect

            if (y > WINDOW_HEIGHT) {
                y = -length;
                x = random.nextFloat() * WINDOW_WIDTH;
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(new Color(173, 216, 230, (int) opacity));
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine((int) x, (int) y, (int) x, (int) (y + length));
        }
    }

    // Cloud class
    class Cloud {
        float x, y, size;
        float opacity = 100;
        boolean dispersing = false;
        float disperseSpeed = 0;
        float driftX = 0;
        float driftY = 0;
        float age = 0;

        public Cloud(float x, float y, float size) {
            this(x, y, size, false);
        }

        public Cloud(float x, float y, float size, boolean dispersing) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.dispersing = dispersing;
            this.driftX = (random.nextFloat() - 0.5f) * 0.2f;
            this.driftY = random.nextFloat() * 0.05f;
            if (dispersing) {
                this.disperseSpeed = 1.0f + random.nextFloat() * 1.0f;
                this.opacity = 150;
            }
        }

        public void update() {
            age++;

            if (!dispersing) {
                // Slow drift for background clouds
                x += driftX;
                y += driftY;

                // Wrap around screen
                if (x < -size)
                    x = WINDOW_WIDTH + size;
                if (x > WINDOW_WIDTH + size)
                    x = -size;
                if (y > WINDOW_HEIGHT)
                    y = -size;
            } else {
                // Dispersing clouds expand and fade
                size += disperseSpeed;
                opacity -= 2.0f;
                disperseSpeed *= 0.985f;

                // Slight upward drift
                y -= 0.3f;
            }
        }

        public boolean shouldRemove() {
            return dispersing && opacity <= 0;
        }

        public void draw(Graphics2D g2d) {
            if (opacity <= 0)
                return;

            Color cloudColor = new Color(255, 255, 255, (int) Math.max(0, opacity));
            g2d.setColor(cloudColor);

            // Main cloud body
            g2d.fill(new Ellipse2D.Float(x - size / 2, y - size / 3, size, size / 1.5f));

            // Additional cloud puffs
            float puffSize = size * 0.6f;
            g2d.fill(new Ellipse2D.Float(x - size / 4, y - size / 5, puffSize, puffSize * 0.7f));
            g2d.fill(new Ellipse2D.Float(x + size / 8, y - size / 6, puffSize * 0.8f, puffSize * 0.6f));

            // Soft outer glow
            Color softEdge = new Color(255, 255, 255, (int) Math.max(0, opacity / 5));
            g2d.setColor(softEdge);
            g2d.fill(new Ellipse2D.Float(x - size / 2 - 6, y - size / 3 - 4, size + 12, size / 1.5f + 8));
        }
    }

    // VendingMachine class
    class VendingMachine {
        float x, y;
        float fallDistance = 0; // 0 = far away (small), 1 = on ground (full size)
        float rotation = 0;
        float rotationSpeed;
        float velocityY = 1.0f;
        boolean onGround = false;
        int timeOnGround = 0;
        private static final float MAX_FALL_DISTANCE = WINDOW_HEIGHT + 100;
        private static final int BASE_WIDTH = 70;
        private static final int BASE_HEIGHT = 100;

        public VendingMachine(float x, float y) {
            this.x = x;
            this.y = y;
            this.rotationSpeed = (random.nextFloat() - 0.5f) * 0.03f;

            // Create portal particles at spawn
            for (int i = 0; i < 8; i++) {
                portalParticles.add(new PortalParticle(x + (random.nextFloat() - 0.5f) * 40, 60));
            }
        }

        public void update() {
            if (!onGround) {
                y += velocityY;
                fallDistance = Math.min(1.0f, (y + 100) / MAX_FALL_DISTANCE);
                rotation += rotationSpeed;

                // Accelerate as falling
                velocityY += 0.04f;

                // Slow down as approaching ground
                if (fallDistance > 0.8f) {
                    velocityY *= 0.96f;
                    rotationSpeed *= 0.96f;
                }

                // Check if reached ground
                if (y >= WINDOW_HEIGHT - 180) {
                    onGround = true;
                    y = WINDOW_HEIGHT - 180;
                    velocityY = 0;
                    rotationSpeed = 0;
                    rotation = 0; // settle upright

                    // Create crash particles
                    for (int i = 0; i < 12; i++) {
                        crashParticles.add(new CrashParticle(x + (random.nextFloat() - 0.5f) * 60, y + 20));
                    }

                    // Create dispersing cloud
                    clouds.add(new Cloud(x + (random.nextFloat() - 0.5f) * 80, y - 30, 50, true));
                }
            } else {
                timeOnGround++;
                // Gentle settling animation
                if (timeOnGround < 45) {
                    y += Math.sin(timeOnGround * 0.2f) * 0.3f;
                }
            }
        }

        private float getScale() {
            return 0.2f + (fallDistance * 0.8f);
        }

        private float getGroundY() {
            if (onGround)
                return y;

            float skyY = 60;
            float groundY = WINDOW_HEIGHT - 180;
            return skyY + (groundY - skyY) * fallDistance;
        }

        public void draw(Graphics2D g2d) {
            float scale = getScale();
            float currentWidth = BASE_WIDTH * scale;
            float currentHeight = BASE_HEIGHT * scale;
            float drawY = getGroundY();

            AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(x, drawY);
            g2d.rotate(rotation);

            if (scale < 0.3f) {
                // Simple distant shape
                g2d.setColor(new Color(180, 140, 140, (int) (255 * Math.min(1.0f, scale * 4))));
                g2d.fill(new Rectangle2D.Float(-currentWidth / 2, -currentHeight / 2, currentWidth, currentHeight));
            } else {
                // Detailed vending machine
                // Main body
                g2d.setColor(VENDING_BASE);
                g2d.fill(new RoundRectangle2D.Float(-currentWidth / 2, -currentHeight / 2, currentWidth, currentHeight,
                        8 * scale, 8 * scale));

                // Front panel
                g2d.setColor(VENDING_ACCENT);
                g2d.fill(new RoundRectangle2D.Float(-currentWidth / 2 + 4 * scale, -currentHeight / 2 + 8 * scale,
                        currentWidth - 8 * scale, currentHeight / 1.8f, 5 * scale, 5 * scale));

                // Glass section
                g2d.setColor(new Color(200, 240, 255, 120));
                g2d.fill(new RoundRectangle2D.Float(-currentWidth / 2 + 7 * scale, -currentHeight / 2 + 12 * scale,
                        currentWidth - 14 * scale, currentHeight / 2.2f, 3 * scale, 3 * scale));

                // Product shelves
                if (scale > 0.5f) {
                    g2d.setColor(new Color(255, 255, 255, 100));
                    for (int i = 0; i < 4; i++) {
                        g2d.fill(new Rectangle2D.Float(-currentWidth / 2 + 9 * scale,
                                -currentHeight / 3 + i * 7 * scale,
                                currentWidth - 18 * scale, 1.5f * scale));
                    }

                    // Selection buttons
                    g2d.setColor(new Color(120, 120, 120));
                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < 3; j++) {
                            g2d.fill(new Ellipse2D.Float(-12 * scale + j * 8 * scale,
                                    8 * scale + i * 8 * scale,
                                    5 * scale, 5 * scale));
                        }
                    }

                    // Brand text
                    g2d.setColor(new Color(255, 255, 255, 160));
                    g2d.setFont(new Font("Arial", Font.BOLD, Math.max(8, (int) (6 * scale))));
                    FontMetrics fm = g2d.getFontMetrics();
                    String brand = "SIDE";
                    int brandWidth = fm.stringWidth(brand);
                    g2d.drawString(brand, -brandWidth / 2, -currentHeight / 2.5f);
                }

                // Coin slot and dispenser
                g2d.setColor(new Color(0, 0, 0, 120));
                g2d.fill(new RoundRectangle2D.Float(-8 * scale, currentHeight / 2.5f - 4 * scale,
                        16 * scale, 5 * scale, 2 * scale, 2 * scale));
            }

            g2d.setTransform(oldTransform);
        }

        public void drawShadow(Graphics2D g2d) {
            if (fallDistance < 0.5f)
                return;

            float scale = getScale();
            float shadowScale = scale * (fallDistance * fallDistance);
            float shadowOpacity = Math.min(50, 50 * shadowScale);

            g2d.setColor(new Color(0, 0, 0, (int) shadowOpacity));
            g2d.fill(new Ellipse2D.Float(
                    x - BASE_WIDTH / 2 * shadowScale,
                    WINDOW_HEIGHT - 125,
                    BASE_WIDTH * shadowScale,
                    BASE_HEIGHT / 5 * shadowScale));
        }
    }
}