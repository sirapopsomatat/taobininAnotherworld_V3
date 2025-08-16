package projectCG;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
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
    private Timer dispensingTimer;
    private float time = 0;
    private List<FallingItem> fallingItems;
    private List<Particle> particles;
    private List<GiftBox> giftBoxes; // For Santa's gifts in Snow weather
    private Random random;
    private Point lastClickedItem = null;
    private int floorY; // Ground level for physics

    private static JFrame frame;

    public static void setFrame(JFrame f) {
        frame = f;
    }

    public static JFrame getFrame() {
        return frame;
    }

    public void startAnimation() {
        if (timer != null && !timer.isRunning()) {
            timer.start();
        }
    }

    // Animation states
    private boolean machineDropped = false;
    private float machineY = -500; // Start above screen
    private float machineTargetY;
    private float machineVelocityY = 0;
    private boolean machineOnGround = false;

    // Weather system
    private String[] weatherEvents = {
            "Sunny Day", "Partly Cloudy", "Overcast",
            "Light Rain", "Thunderstorm", "Rainbow",
            "Spring Breeze", "Snow Fall", "Autumn Leaves",
            "Starry Night", "Dawn Break", "Sunset Glow"
    };
    private String currentWeather;
    private float weatherChangeTimer = 0;
    private float weatherDuration = 600; // frames (3 seconds instead of 5-10)

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
        String text;
        boolean isVisible;
        float alpha = 1.0f;
        float size = 20f;
        boolean onGround = false;
        float bounciness = 0.6f;
        float friction = 0.8f;

        public FallingItem(float x, float y, Color color, String text) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.text = text;
            this.vx = (float) (Math.random() - 0.5) * 2;
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
                    vy += 0.8f; // Much stronger gravity!
                }

                rotation += rotationSpeed;

                // Ground collision
                if (y + size / 2 >= floorY && vy > 0) {
                    y = floorY - size / 2;
                    vy *= -bounciness; // Bounce
                    vx *= friction; // Friction
                    rotationSpeed *= 0.9f;

                    if (Math.abs(vy) < 1.5f) { // Increased threshold for faster settling
                        vy = 0;
                        onGround = true;
                    }
                }

                // Wall collisions
                if (x - size / 2 <= 0 && vx < 0) {
                    x = size / 2;
                    vx *= -bounciness;
                } else if (x + size / 2 >= frameWidth && vx > 0) {
                    x = frameWidth - size / 2;
                    vx *= -bounciness;
                }

                // Apply friction when on ground
                if (onGround) {
                    vx *= 0.95f;
                    if (Math.abs(vx) < 0.1f) {
                        vx = 0;
                    }
                }
            }
        }

        // Check collision with another item
        public boolean collidesWith(FallingItem other) {
            float dx = x - other.x;
            float dy = y - other.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            return distance < (size + other.size) / 2;
        }

        // Handle collision with another item
        public void handleCollision(FallingItem other) {
            float dx = x - other.x;
            float dy = y - other.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < (size + other.size) / 2 && distance > 0) {
                // Normalize collision vector
                dx /= distance;
                dy /= distance;

                // Separate objects
                float overlap = (size + other.size) / 2 - distance;
                x += dx * overlap * 0.5f;
                y += dy * overlap * 0.5f;
                other.x -= dx * overlap * 0.5f;
                other.y -= dy * overlap * 0.5f;

                // Exchange velocities (simplified elastic collision)
                float tempVx = vx;
                float tempVy = vy;

                vx = (vx + other.vx) * 0.5f + dx * Math.abs(other.vx - vx) * 0.5f;
                vy = (vy + other.vy) * 0.5f + dy * Math.abs(other.vy - vy) * 0.5f;

                other.vx = (tempVx + other.vx) * 0.5f - dx * Math.abs(tempVx - other.vx) * 0.5f;
                other.vy = (tempVy + other.vy) * 0.5f - dy * Math.abs(tempVy - other.vy) * 0.5f;

                // Add some energy loss
                vx *= 0.8f;
                vy *= 0.8f;
                other.vx *= 0.8f;
                other.vy *= 0.8f;
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

    // New class for Santa's gift boxes
    private static class GiftBox {
        float x, y, vx, vy, rotation, rotationSpeed;
        Color color;
        boolean isVisible;
        float alpha = 1.0f;
        float size = 15f;
        boolean onGround = false;
        float bounciness = 0.7f;
        float friction = 0.85f;

        public GiftBox(float x, float y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = (float) (Math.random() - 0.5) * 3;
            this.vy = 1 + (float) Math.random() * 2;
            this.rotation = 0;
            this.rotationSpeed = (float) (Math.random() - 0.5) * 0.15f;
            this.isVisible = true;
        }

        public void update(int floorY, int frameWidth) {
            if (isVisible) {
                x += vx;
                y += vy;

                if (!onGround) {
                    vy += 0.6f; // Gravity for gift boxes
                }

                rotation += rotationSpeed;

                // Ground collision
                if (y + size / 2 >= floorY && vy > 0) {
                    y = floorY - size / 2;
                    vy *= -bounciness;
                    vx *= friction;
                    rotationSpeed *= 0.9f;

                    if (Math.abs(vy) < 1.0f) {
                        vy = 0;
                        onGround = true;
                    }
                }

                // Wall collisions
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

    public LofiTaoBinVendingMachine() {
        setPreferredSize(new Dimension(600, 600));
        setBackground(new Color(255, 240, 245));

        fallingItems = new ArrayList<>();
        particles = new ArrayList<>();
        giftBoxes = new ArrayList<>();
        random = new Random();

        // Calculate floor position - move closer to bottom
        floorY = 550; // Moved down from 520

        // Calculate machine target position - make it stick to ground more
        machineTargetY = floorY - 420 + 10; // Stick closer to ground (10px overlap)

        // Start machine dropping immediately!
        machineDropped = true;
        machineVelocityY = 0;

        // Initialize weather with random selection
        currentWeather = weatherEvents[random.nextInt(weatherEvents.length)];
        weatherChangeTimer = 0;

        animationTimer = new Timer(16, this); // ~60 FPS
        animationTimer.start();

        addMouseListener(this);

        // Add some initial particles
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(random.nextFloat() * 600, random.nextFloat() * 600));
        }
    }

    private void dispenseRandomItem() {
        // Random color from lofi palette
        Color itemColor = lofiColors[random.nextInt(lofiColors.length)];

        // Calculate dispensing area (ทำให้กว้างขึ้น)
        int machineX = (getWidth() - 200) / 2;
        int machineY = (int) machineTargetY;

        // Dispensing area coordinates (ช่องออกกว้าง 120px แทน 40px)
        int areaX = machineX + 40; // เริ่มต้นจากซ้าย
        int areaWidth = 120; // ความกว้างของช่องออก
        int areaY = machineY + 340; // ความสูงช่องออก
        int areaHeight = 25; // ความสูงช่องออก

        // Random position ทั่วทั้ง area ของช่องออก
        float randomX = areaX + random.nextFloat() * areaWidth;
        float randomY = areaY + random.nextFloat() * areaHeight;

        // Create falling item from random position in dispensing area
        FallingItem newItem = new FallingItem(randomX, randomY, itemColor, "Drink");

        // เพิ่ม variety ในการเคลื่อนไหว
        newItem.vy = 1.5f + random.nextFloat() * 0.5f; // ความเร็วตกแนวตั้ง
        newItem.vx = (float) (Math.random() - 0.5) * 10f; // ความเร็วแนวนอนเพิ่มขึ้น
        newItem.rotationSpeed = (float) (Math.random() - 0.5) * 0.2f; // หมุนเร็วขึ้น

        // เพิ่ม random size เล็กน้อย
        newItem.size = 15f + random.nextFloat() * 9f; // ขนาด 18-24

        fallingItems.add(newItem);

        // Dispensing particles กระจายทั่ว area
        for (int i = 0; i < 15; i++) {
            float particleX = areaX + random.nextFloat() * areaWidth;
            float particleY = areaY + random.nextFloat() * areaHeight;

            Particle sparkle = new Particle(particleX, particleY);
            sparkle.vx = (float) (Math.random() - 0.5) * 5;
            sparkle.vy = (float) (Math.random() - 0.5) * 4 - 1;
            sparkle.color = new Color(
                    itemColor.getRed(),
                    itemColor.getGreen(),
                    itemColor.getBlue(),
                    180);
            sparkle.size = random.nextFloat() * 4 + 1;
            particles.add(sparkle);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw animated lofi sky background

        drawLofiSkyBackground(g2d);
        // Draw weather decorations AFTER sky but BEFORE other elements

        drawWeatherDecorations(g2d);
        drawLofiGreenGlass(g2d, floorY - 50);

        // Draw floating particles
        drawParticles(g2d);

        // Draw weather info in top right corner
        drawWeatherInfo(g2d);

        // Draw vertical Japanese text with animation
        drawAnimatedVerticalJapaneseText(g2d, "立方体の自動販売機", 50, 50, 20);

        // Calculate machine position
        int cubeWidth = 200;
        int cubeHeight = 420;
        int machineX = (getWidth() - cubeWidth) / 2;
        int currentMachineY = (int) machineY;

        // Draw dreamy glass floor
        drawLofiGreenGlass(g2d, floorY - 50);

        // Only draw machine if it has started dropping or is on ground (always true
        // now)
        // Draw soft shadow behind machine
        drawLofiCubeShadow(g2d, machineX + 20, currentMachineY - 20, cubeWidth, cubeHeight);

        // Draw the cubic vending machine with lofi colors
        drawLofiCubicVendingMachine(g2d, machineX, currentMachineY, cubeWidth, cubeHeight);

        // Draw falling items only if machine is on ground
        if (machineOnGround) {
            drawFallingItems(g2d);
            // Draw gift boxes for Snow weather
            if (currentWeather.equals("Snow Fall")) {
                drawGiftBoxes(g2d);
            }
        }

        // Draw item count
        if (machineOnGround) {
            g2d.setColor(new Color(100, 100, 100, 150));
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Items: " + fallingItems.size(), 10, getHeight() - 20);
        }
    }

    // NEW: Weather decorations method
    private void drawWeatherDecorations(Graphics2D g2d) {
        switch (currentWeather) {
            case "Rainbow":
                drawRainbow(g2d);
                break;
            case "Sunny Day":
                drawSun(g2d);
                break;
            case "Snow Fall":
                // Gift boxes are drawn separately in main paint method
                break;
            case "Starry Night":
                drawMoon(g2d);
                drawStars(g2d);
                break;
            case "Dawn Break":
                drawSunrise(g2d);
                break;
            case "Sunset Glow":
                drawSunset(g2d);
                break;
            case "Thunderstorm":
                drawLightning(g2d);
                break;
            case "Spring Breeze":
                drawCherryBlossoms(g2d);
                break;
            case "Autumn Leaves":
                drawFallingLeaves(g2d);
                break;
            case "Partly Cloudy":
                drawPartialSun(g2d);
                break;
        }
    }

    private void drawRainbow(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() + 50;
        Color[] rainbowColors = {
                new Color(255, 0, 0, 150), // Red
                new Color(255, 165, 0, 150), // Orange
                new Color(255, 255, 0, 150), // Yellow
                new Color(0, 255, 0, 150), // Green
                new Color(0, 0, 255, 150), // Blue
                new Color(75, 0, 130, 150), // Indigo
                new Color(148, 0, 211, 150) // Violet
        };

        g2d.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Draw rainbow arcs
        for (int i = 0; i < rainbowColors.length; i++) {
            g2d.setColor(rainbowColors[i]);
            int radius = 280 - i * 15;
            g2d.drawArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 0, 180);
        }

        // Add sparkle effects on rainbow
        g2d.setColor(new Color(255, 255, 255, (int) (200 + 50 * Math.sin(time * 0.1f))));
        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI / 7);
            int sparkleX = (int) (centerX + 200 * Math.cos(angle));
            int sparkleY = (int) (centerY - 200 * Math.sin(angle));
            drawSparkle(g2d, sparkleX, sparkleY, 4);
        }
    }

    private void drawSun(Graphics2D g2d) {
        int sunX = getWidth() - 100;
        int sunY = 80;
        int sunSize = 60;

        // Sun body with gradient
        RadialGradientPaint sunGradient = new RadialGradientPaint(
                sunX, sunY, sunSize / 2,
                new float[] { 0f, 1f },
                new Color[] { new Color(255, 255, 100, 200), new Color(255, 200, 50, 150) });
        g2d.setPaint(sunGradient);
        g2d.fillOval(sunX - sunSize / 2, sunY - sunSize / 2, sunSize, sunSize);

        // Sun rays
        g2d.setColor(new Color(255, 220, 100, (int) (180 + 70 * Math.sin(time * 0.05f))));
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int i = 0; i < 12; i++) {
            float angle = (float) (i * Math.PI / 6 + time * 0.02f);
            int rayLength = 40 + (int) (10 * Math.sin(time * 0.1f + i));
            int rayStartX = (int) (sunX + (sunSize / 2 + 10) * Math.cos(angle));
            int rayStartY = (int) (sunY + (sunSize / 2 + 10) * Math.sin(angle));
            int rayEndX = (int) (sunX + (sunSize / 2 + rayLength) * Math.cos(angle));
            int rayEndY = (int) (sunY + (sunSize / 2 + rayLength) * Math.sin(angle));

            g2d.drawLine(rayStartX, rayStartY, rayEndX, rayEndY);
        }

        // Sun face
        g2d.setColor(new Color(255, 150, 0));
        // Eyes
        g2d.fillOval(sunX - 15, sunY - 10, 6, 6);
        g2d.fillOval(sunX + 9, sunY - 10, 6, 6);
        // Smile
        g2d.setStroke(new BasicStroke(2));
        g2d.drawArc(sunX - 12, sunY - 5, 24, 20, 0, -180);
    }

    private void drawMoon(Graphics2D g2d) {
        int moonX = getWidth() - 120;
        int moonY = 100;
        int moonSize = 80;

        // Full moon body
        g2d.setColor(new Color(240, 240, 200, (int) (200 + 50 * Math.sin(time * 0.03f))));
        g2d.fillOval(moonX - moonSize / 2, moonY - moonSize / 2, moonSize, moonSize);

        // Moon glow
        RadialGradientPaint moonGlow = new RadialGradientPaint(
                moonX, moonY, moonSize,
                new float[] { 0f, 1f },
                new Color[] { new Color(255, 255, 220, 100), new Color(255, 255, 220, 0) });
        g2d.setPaint(moonGlow);
        g2d.fillOval(moonX - moonSize, moonY - moonSize, moonSize * 2, moonSize * 2);

        // Moon craters
        g2d.setColor(new Color(200, 200, 180, 100));
        g2d.fillOval(moonX - 15, moonY - 10, 8, 6);
        g2d.fillOval(moonX + 5, moonY + 8, 12, 8);
        g2d.fillOval(moonX - 8, moonY + 15, 6, 4);

        // Moon face (sleepy)
        g2d.setColor(new Color(180, 180, 160));
        // Sleepy eyes (closed)
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(moonX - 15, moonY - 8, moonX - 10, moonY - 8);
        g2d.drawLine(moonX + 10, moonY - 8, moonX + 15, moonY - 8);
        // Peaceful smile
        g2d.drawArc(moonX - 10, moonY + 5, 20, 15, 0, -180);
    }

    private void drawStars(Graphics2D g2d) {
        // Twinkling stars around the moon
        for (int i = 0; i < 15; i++) {
            float twinkle = (float) Math.sin(time * 0.08f + i) * 0.5f + 0.5f;
            g2d.setColor(new Color(255, 255, 200, (int) (150 * twinkle + 50)));

            int starX = 50 + i * 35 + (int) (20 * Math.sin(time * 0.02f + i));
            int starY = 50 + (int) (30 * Math.cos(time * 0.015f + i * 0.5f));

            // Skip stars too close to moon
            if (Math.sqrt(Math.pow(starX - (getWidth() - 120), 2) + Math.pow(starY - 100, 2)) > 100) {
                drawStar(g2d, starX, starY, (int) (4 + 2 * twinkle));
            }
        }
    }

    private void drawSunrise(Graphics2D g2d) {
        int sunX = 120;
        int sunY = getHeight() - 100; // Lower position for sunrise
        int sunSize = 70;

        // Sunrise sun with warm colors
        RadialGradientPaint sunriseGradient = new RadialGradientPaint(
                sunX, sunY, sunSize / 2,
                new float[] { 0f, 1f },
                new Color[] { new Color(255, 180, 80, 180), new Color(255, 120, 60, 120) });
        g2d.setPaint(sunriseGradient);
        g2d.fillOval(sunX - sunSize / 2, sunY - sunSize / 2, sunSize, sunSize);

        // Sunrise rays (longer and more dramatic)
        g2d.setColor(new Color(255, 150, 80, (int) (150 + 100 * Math.sin(time * 0.04f))));
        g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int i = 0; i < 16; i++) {
            float angle = (float) (i * Math.PI / 8 + time * 0.015f);
            int rayLength = 60 + (int) (20 * Math.sin(time * 0.08f + i));
            int rayStartX = (int) (sunX + (sunSize / 2 + 15) * Math.cos(angle));
            int rayStartY = (int) (sunY + (sunSize / 2 + 15) * Math.sin(angle));
            int rayEndX = (int) (sunX + (sunSize / 2 + rayLength) * Math.cos(angle));
            int rayEndY = (int) (sunY + (sunSize / 2 + rayLength) * Math.sin(angle));

            g2d.drawLine(rayStartX, rayStartY, rayEndX, rayEndY);
        }
    }

    private void drawSunset(Graphics2D g2d) {
        int sunX = 80;
        int sunY = getHeight() - 80; // Very low position for sunset
        int sunSize = 90;

        // Large sunset sun with warm gradient
        RadialGradientPaint sunsetGradient = new RadialGradientPaint(
                sunX, sunY, sunSize / 2,
                new float[] { 0f, 1f },
                new Color[] { new Color(255, 100, 50, 200), new Color(255, 60, 30, 150) });
        g2d.setPaint(sunsetGradient);
        g2d.fillOval(sunX - sunSize / 2, sunY - sunSize / 2, sunSize, sunSize);

        // Sunset reflection effect
        GradientPaint reflectionGradient = new GradientPaint(
                sunX, sunY, new Color(255, 100, 50, 100),
                sunX, getHeight(), new Color(255, 100, 50, 20));
        g2d.setPaint(reflectionGradient);
        g2d.fillOval(sunX - sunSize / 3, sunY, sunSize * 2 / 3, getHeight() - sunY);
    }

    private void drawLightning(Graphics2D g2d) {
        // Random lightning bolts
        if (random.nextFloat() < 0.1f) { // 10% chance each frame for lightning
            g2d.setColor(new Color(255, 255, 255, (int) (200 + 50 * Math.sin(time * 0.5f))));
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Draw zigzag lightning
            int startX = random.nextInt(getWidth());
            int startY = 20;
            int currentX = startX;
            int currentY = startY;

            for (int i = 0; i < 6; i++) {
                int nextX = currentX + random.nextInt(60) - 30;
                int nextY = currentY + 30 + random.nextInt(40);
                g2d.drawLine(currentX, currentY, nextX, nextY);
                currentX = nextX;
                currentY = nextY;

                if (currentY > getHeight() / 2)
                    break;
            }
        }
    }

    private void drawCherryBlossoms(Graphics2D g2d) {
        // Floating cherry blossom petals
        g2d.setColor(new Color(255, 192, 203, 150));
        for (int i = 0; i < 25; i++) {
            float petalX = (float) (i * 25 + 15 * Math.sin(time * 0.06f + i));
            float petalY = (float) ((time * 1.2f + i * 20) % getHeight());

            // Draw cherry blossom petal (5 petals)
            drawCherryBlossomPetal(g2d, (int) petalX, (int) petalY);
        }
    }

    private void drawCherryBlossomPetal(Graphics2D g2d, int x, int y) {
        g2d.setColor(new Color(255, 182, 193, (int) (150 + 50 * Math.sin(time * 0.1f))));

        // Draw 5 petals in a circle
        for (int i = 0; i < 5; i++) {
            float angle = (float) (i * 2 * Math.PI / 5 + time * 0.02f);
            int petalX = (int) (x + 8 * Math.cos(angle));
            int petalY = (int) (y + 8 * Math.sin(angle));
            g2d.fillOval(petalX - 4, petalY - 6, 8, 12);
        }

        // Center
        g2d.setColor(new Color(255, 255, 100, 180));
        g2d.fillOval(x - 2, y - 2, 4, 4);
    }

    private void drawFallingLeaves(Graphics2D g2d) {
        // Autumn leaves falling
        Color[] leafColors = {
                new Color(255, 140, 0, 150), // Dark orange
                new Color(255, 165, 0, 150), // Orange
                new Color(178, 34, 34, 150), // Fire brick
                new Color(205, 133, 63, 150), // Peru
                new Color(139, 69, 19, 150) // Saddle brown
        };

        for (int i = 0; i < 20; i++) {
            float leafX = (float) (i * 30 + 20 * Math.sin(time * 0.08f + i));
            float leafY = (float) ((time * 1.5f + i * 25) % getHeight());
            Color leafColor = leafColors[i % leafColors.length];

            g2d.setColor(leafColor);
            drawLeaf(g2d, (int) leafX, (int) leafY);
        }
    }

    private void drawPartialSun(Graphics2D g2d) {
        // Sun partially hidden by clouds
        int sunX = getWidth() - 150;
        int sunY = 120;
        int sunSize = 50;

        // Partial sun
        RadialGradientPaint partialSunGradient = new RadialGradientPaint(
                sunX, sunY, sunSize / 2,
                new float[] { 0f, 1f },
                new Color[] { new Color(255, 255, 150, 180), new Color(255, 200, 100, 120) });
        g2d.setPaint(partialSunGradient);
        g2d.fillOval(sunX - sunSize / 2, sunY - sunSize / 2, sunSize, sunSize);

        // Some sun rays peeking through
        g2d.setColor(new Color(255, 220, 100, 120));
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI / 4 + time * 0.02f);
            if (i % 2 == 0) { // Only some rays visible
                int rayLength = 25 + (int) (8 * Math.sin(time * 0.1f + i));
                int rayStartX = (int) (sunX + (sunSize / 2 + 8) * Math.cos(angle));
                int rayStartY = (int) (sunY + (sunSize / 2 + 8) * Math.sin(angle));
                int rayEndX = (int) (sunX + (sunSize / 2 + rayLength) * Math.cos(angle));
                int rayEndY = (int) (sunY + (sunSize / 2 + rayLength) * Math.sin(angle));

                g2d.drawLine(rayStartX, rayStartY, rayEndX, rayEndY);
            }
        }
    }

    private void drawSparkle(Graphics2D g2d, int x, int y, int size) {
        // Draw a sparkle/star shape
        g2d.fillOval(x - size / 2, y - size / 2, size, size);
        g2d.drawLine(x - size, y, x + size, y);
        g2d.drawLine(x, y - size, x, y + size);
    }

    private void drawGiftBoxes(Graphics2D g2d) {
        for (GiftBox gift : giftBoxes) {
            if (gift.isVisible) {
                AffineTransform oldTransform = g2d.getTransform();
                g2d.translate(gift.x, gift.y);
                g2d.rotate(gift.rotation);

                // Gift box body
                int halfSize = (int) (gift.size / 2);
                g2d.setColor(gift.color);
                g2d.fillRoundRect(-halfSize, -halfSize, (int) gift.size, (int) gift.size, 3, 3);

                // Gift ribbon
                g2d.setColor(Color.RED);
                g2d.fillRect(-halfSize, -2, (int) gift.size, 4); // Horizontal ribbon
                g2d.fillRect(-2, -halfSize, 4, (int) gift.size); // Vertical ribbon

                // Gift bow
                g2d.fillOval(-4, -halfSize - 2, 8, 6);

                g2d.setTransform(oldTransform);
            }
        }
    }

    // แก้ไข: เพิ่ม method drawLofiSkyBackground ที่หายไป + เปลี่ยนตาม season
    private void drawLofiSkyBackground(Graphics2D g2d) {
        float t = time * 0.01f;
        Color topColor, bottomColor;

        // เปลี่ยนสีฟ้าตาม season/weather
        if (currentWeather.contains("Night") || currentWeather.contains("Starry")) {
            // กลางคืน - สีม่วงเข้ม
            topColor = new Color(
                    (int) (25 + 15 * Math.sin(t)),
                    (int) (25 + 20 * Math.cos(t * 0.8f)),
                    (int) (60 + 30 * Math.sin(t * 1.2f)));
            bottomColor = new Color(
                    (int) (60 + 20 * Math.sin(t * 0.5f)),
                    (int) (30 + 25 * Math.cos(t * 0.7f)),
                    (int) (80 + 15 * Math.sin(t * 0.9f)));
        } else if (currentWeather.contains("Rain") || currentWeather.contains("Thunder")) {
            // ฝนตก - สีเทาเข้ม
            topColor = new Color(
                    (int) (80 + 20 * Math.sin(t)),
                    (int) (80 + 25 * Math.cos(t * 0.8f)),
                    (int) (90 + 20 * Math.sin(t * 1.2f)));
            bottomColor = new Color(
                    (int) (120 + 30 * Math.sin(t * 0.5f)),
                    (int) (120 + 35 * Math.cos(t * 0.7f)),
                    (int) (130 + 25 * Math.sin(t * 0.9f)));
        } else if (currentWeather.contains("Snow")) {
            // หิมะ - สีขาวฟ้าอ่อน
            topColor = new Color(
                    (int) (200 + 25 * Math.sin(t)),
                    (int) (220 + 20 * Math.cos(t * 0.8f)),
                    (int) (255));
            bottomColor = new Color(
                    (int) (240 + 15 * Math.sin(t * 0.5f)),
                    (int) (245 + 10 * Math.cos(t * 0.7f)),
                    (int) (255));
        } else if (currentWeather.contains("Sunset") || currentWeather.contains("Dawn")) {
            // พระอาทิตย์ตก/ขึ้น - สีส้มชมพู
            topColor = new Color(
                    (int) (255),
                    (int) (150 + 50 * Math.cos(t * 0.8f)),
                    (int) (100 + 30 * Math.sin(t * 1.2f)));
            bottomColor = new Color(
                    (int) (255 - 30 * Math.sin(t * 0.5f)),
                    (int) (200 + 40 * Math.cos(t * 0.7f)),
                    (int) (150 + 60 * Math.sin(t * 0.9f)));
        } else if (currentWeather.contains("Autumn")) {
            // ใบไม้ร่วง - สีน้าตาลส้ม
            topColor = new Color(
                    (int) (180 + 40 * Math.sin(t)),
                    (int) (120 + 50 * Math.cos(t * 0.8f)),
                    (int) (60 + 25 * Math.sin(t * 1.2f)));
            bottomColor = new Color(
                    (int) (220 + 20 * Math.sin(t * 0.5f)),
                    (int) (160 + 30 * Math.cos(t * 0.7f)),
                    (int) (80 + 40 * Math.sin(t * 0.9f)));
        } else if (currentWeather.contains("Spring")) {
            // ฤดูใบไม้ผลิ - สีเขียวอ่อน
            topColor = new Color(
                    (int) (150 + 40 * Math.sin(t)),
                    (int) (220 + 25 * Math.cos(t * 0.8f)),
                    (int) (180 + 35 * Math.sin(t * 1.2f)));
            bottomColor = new Color(
                    (int) (200 + 25 * Math.sin(t * 0.5f)),
                    (int) (255 - 15 * Math.cos(t * 0.7f)),
                    (int) (220 + 20 * Math.sin(t * 0.9f)));
        } else {
            // ปกติ - ฟ้าใส
            topColor = new Color(
                    (int) (135 + 50 * Math.sin(t)),
                    (int) (206 + 30 * Math.cos(t * 0.8f)),
                    (int) (250 + 5 * Math.sin(t * 1.2f)));
            bottomColor = new Color(
                    (int) (255 - 20 * Math.sin(t * 0.5f)),
                    (int) (240 + 15 * Math.cos(t * 0.7f)),
                    (int) (245 + 10 * Math.sin(t * 0.9f)));
        }

        GradientPaint skyGradient = new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor);
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Add animated clouds ที่เปลี่ยนสีตาม weather
        drawAnimatedClouds(g2d);

        // เพิ่มเอฟเฟกต์พิเศษตาม weather
        drawWeatherBackgroundEffects(g2d);
    }

    private void drawWeatherInfo(Graphics2D g2d) {
        // Weather info panel in top right corner
        int panelWidth = 180;
        int panelHeight = 80;
        int panelX = getWidth() - panelWidth - 20;
        int panelY = 20;

        // Semi-transparent background with lofi colors
        Color panelBg = new Color(255, 255, 255, 200);
        g2d.setColor(panelBg);
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 15, 15);

        // Border with animated color
        float t = time * 0.01f;
        Color borderColor = new Color(
                (int) (200 + 50 * Math.sin(t)),
                (int) (150 + 80 * Math.cos(t * 0.8f)),
                (int) (255 - 30 * Math.sin(t * 1.2f)),
                180);
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 15, 15);

        // Weather title
        g2d.setColor(new Color(80, 80, 100));
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Current Weather:", panelX + 10, panelY + 20);

        // Current weather with larger font
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(new Color(60, 60, 80));
        g2d.drawString(currentWeather, panelX + 10, panelY + 40);

        // Weather change countdown
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.setColor(new Color(120, 120, 140));
        int remainingTime = (int) ((weatherDuration - weatherChangeTimer) / 60); // Convert to seconds
        String countdownText = "Changes in: " + Math.max(0, remainingTime) + "s";

        // Add highlight when about to change
        if (remainingTime <= 1) {
            g2d.setColor(new Color(255, 100, 100)); // Red warning
            countdownText = "CHANGING...";
        }

        g2d.drawString(countdownText, panelX + 10, panelY + 60);

        // Add small animated weather particles
        drawWeatherEffects(g2d, panelX, panelY, panelWidth, panelHeight);
    }

    private void drawWeatherEffects(Graphics2D g2d, int x, int y, int width, int height) {
        // Add small visual effects based on current weather
        if (currentWeather.contains("Rain") || currentWeather.contains("Thunderstorm")) {
            // Rain drops
            g2d.setColor(new Color(100, 150, 255, 150));
            for (int i = 0; i < 8; i++) {
                int dropX = x + 10 + (i * 15) + (int) (5 * Math.sin(time * 0.1f + i));
                int dropY = y + 15 + (int) (10 * Math.sin(time * 0.08f + i * 0.5f));
                g2d.fillOval(dropX, dropY, 2, 6);
            }
        } else if (currentWeather.contains("Snow")) {
            // Snow flakes
            g2d.setColor(new Color(255, 255, 255, 200));
            for (int i = 0; i < 6; i++) {
                int snowX = x + 15 + (i * 20) + (int) (3 * Math.sin(time * 0.05f + i));
                int snowY = y + 20 + (int) (8 * Math.cos(time * 0.06f + i * 0.7f));
                g2d.fillOval(snowX, snowY, 4, 4);
            }
        } else if (currentWeather.contains("Sunny")) {
            // Sun rays
            g2d.setColor(new Color(255, 220, 100, 180));
            for (int i = 0; i < 6; i++) {
                float angle = (float) (i * Math.PI / 3 + time * 0.02f);
                int rayX = x + width - 30 + (int) (15 * Math.cos(angle));
                int rayY = y + 25 + (int) (15 * Math.sin(angle));
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(x + width - 30, y + 25, rayX, rayY);
            }
        }
    }

    // private void drawIntroMessage(Graphics2D g2d) {
    // // Intro message in center
    // String message = "Click anywhere to drop the vending machine!";
    // g2d.setFont(new Font("Arial", Font.BOLD, 24));
    // FontMetrics fm = g2d.getFontMetrics();
    // int textWidth = fm.stringWidth(message);
    // int textX = (getWidth() - textWidth) / 2;
    // int textY = getHeight() / 2;

    // // Background for text
    // g2d.setColor(new Color(0, 0, 0, 100));
    // g2d.fillRoundRect(textX - 20, textY - 30, textWidth + 40, 50, 25, 25);

    // // Animated text color
    // float t = time * 0.05f;
    // Color textColor = new Color(
    // (int)(255 - 50 * Math.sin(t)),
    // (int)(255 - 30 * Math.cos(t * 0.8f)),
    // (int)(255 - 20 * Math.sin(t * 1.2f))
    // );
    // g2d.setColor(textColor);
    // g2d.drawString(message, textX, textY);
    // }

    private void drawAnimatedClouds(Graphics2D g2d) {
        // เปลี่ยนสีเมฆตาม weather
        Color cloudColor;
        if (currentWeather.contains("Night") || currentWeather.contains("Starry")) {
            cloudColor = new Color(40, 40, 60, 100);
        } else if (currentWeather.contains("Rain") || currentWeather.contains("Thunder")) {
            cloudColor = new Color(60, 60, 70, 150);
        } else if (currentWeather.contains("Snow")) {
            cloudColor = new Color(255, 255, 255, 180);
        } else {
            cloudColor = new Color(255, 255, 255, 120);
        }

        g2d.setColor(cloudColor);
        float t = time * 0.005f;

        // Cloud 1 - floating animation
        int cloud1X = (int) (100 + 20 * Math.sin(t));
        int cloud1Y = (int) (80 + 10 * Math.cos(t * 1.3f));
        drawLofiCloud(g2d, cloud1X, cloud1Y, 80);

        // Cloud 2
        int cloud2X = (int) (480 + 15 * Math.sin(t * 0.8f));
        int cloud2Y = (int) (60 + 8 * Math.cos(t * 1.1f));
        drawLofiCloud(g2d, cloud2X, cloud2Y, 100);

        // Cloud 3
        int cloud3X = (int) (650 + 25 * Math.sin(t * 1.2f));
        int cloud3Y = (int) (100 + 12 * Math.cos(t * 0.9f));
        drawLofiCloud(g2d, cloud3X, cloud3Y, 60);
    }

    // เพิ่มเอฟเฟกต์พื้นหลังตาม weather
    private void drawWeatherBackgroundEffects(Graphics2D g2d) {
        if (currentWeather.contains("Rain") || currentWeather.contains("Thunder")) {
            // ฝนตกพื้นหลัง
            g2d.setColor(new Color(100, 150, 255, 100));
            for (int i = 0; i < 30; i++) {
                int dropX = (int) (i * 25 + 10 * Math.sin(time * 0.1f + i));
                int dropY = (int) ((time * 3 + i * 20) % getHeight());
                g2d.fillOval(dropX, dropY, 2, 8);
            }
        } else if (currentWeather.contains("Snow")) {
            // หิมะตกพื้นหลัง
            g2d.setColor(new Color(255, 255, 255, 200));
            for (int i = 0; i < 25; i++) {
                int snowX = (int) (i * 30 + 15 * Math.sin(time * 0.05f + i));
                int snowY = (int) ((time * 2 + i * 30) % getHeight());
                g2d.fillOval(snowX, snowY, 5, 5);
            }
        } else if (currentWeather.contains("Starry") || currentWeather.contains("Night")) {
            // ดาวระยิบระยับ
            g2d.setColor(new Color(255, 255, 200, (int) (150 + 100 * Math.sin(time * 0.1f))));
            for (int i = 0; i < 15; i++) {
                int starX = 80 + i * 40;
                int starY = 30 + (int) (20 * Math.sin(time * 0.03f + i));
                drawStar(g2d, starX, starY, 3);
            }
        } else if (currentWeather.contains("Autumn")) {
            // ใบไม้ร่วง
            for (int i = 0; i < 20; i++) {
                float leafX = (float) (i * 35 + 20 * Math.sin(time * 0.08f + i));
                float leafY = (float) ((time * 1.5f + i * 25) % getHeight());
                g2d.setColor(new Color(200, 100, 50, 150));
                drawLeaf(g2d, (int) leafX, (int) leafY);
            }
        }
    }

    private void drawStar(Graphics2D g2d, int x, int y, int size) {
        int[] xPoints = { x, x + size / 2, x + size, x + size / 2, x, x - size / 2, x - size, x - size / 2 };
        int[] yPoints = { y - size, y - size / 2, y, y + size / 2, y + size, y + size / 2, y, y - size / 2 };
        g2d.fillPolygon(xPoints, yPoints, 8);
    }

    private void drawLeaf(Graphics2D g2d, int x, int y) {
        g2d.fillOval(x, y, 8, 4);
        g2d.fillOval(x + 2, y - 2, 6, 8);
    }

    private void drawLofiCloud(Graphics2D g2d, int x, int y, int size) {
        // Main cloud body with soft edges
        g2d.fillOval(x, y, size, size / 2);
        g2d.fillOval(x + size / 3, y - size / 6, size / 2, size / 3);
        g2d.fillOval(x - size / 8, y + size / 10, size / 3, size / 4);
        g2d.fillOval(x + size / 2, y + size / 10, size / 3, size / 4);

        // Add soft glow effect
        g2d.setColor(new Color(255, 255, 255, 60));
        g2d.fillOval(x - 10, y - 5, size + 20, size / 2 + 10);
    }

    private void drawLofiGreenGlass(Graphics2D g2d, int floorY) {
        // Create dreamy green glass floor with lofi colors
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

        GradientPaint glassGradient = new GradientPaint(
                0, floorY, glass1,
                0, getHeight(), glass2);
        g2d.setPaint(glassGradient);
        g2d.fillRect(0, floorY, getWidth(), getHeight() - floorY);

        // Add animated shine effect
        Color shine1 = new Color(255, 255, 255, (int) (60 + 20 * Math.sin(time * 0.02f)));
        GradientPaint shine = new GradientPaint(
                0, floorY, shine1,
                0, floorY + 40, new Color(255, 255, 255, 0));
        g2d.setPaint(shine);
        g2d.fillRect(0, floorY, getWidth(), 40);
    }

    private void drawLofiCubeShadow(Graphics2D g2d, int x, int y, int width, int height) {
        // Create soft, dreamy shadow
        int shadowOffset = 20;
        int shadowDepth = 30;

        // Soft shadow with gradient
        RadialGradientPaint shadowGradient = new RadialGradientPaint(
                x + width / 2 + shadowOffset, y + height + shadowOffset,
                width * 0.8f,
                new float[] { 0f, 1f },
                new Color[] { new Color(0, 0, 0, 60), new Color(0, 0, 0, 0) });
        g2d.setPaint(shadowGradient);
        g2d.fillOval(x - 20, y + height - 10, width + 80, 60);
    }

    private void drawLofiCubicVendingMachine(Graphics2D g2d, int x, int y, int width, int height) {
        // Draw the main cube faces in 3D perspective with lofi colors
        drawLofiCubeFrontFace(g2d, x, y, width, height);
        drawLofiCubeRightFace(g2d, x, y, width, height);
        drawLofiCubeTopFace(g2d, x, y, width, height);

        // Add soft cube edges
        drawLofiCubeEdges(g2d, x, y, width, height);
    }

    private void drawLofiCubeFrontFace(Graphics2D g2d, int x, int y, int width, int height) {
        // Front face with dreamy gradient
        float t = time * 0.005f;
        Color front1 = new Color(
                (int) (250 + 5 * Math.sin(t)),
                (int) (245 + 10 * Math.cos(t * 0.8f)),
                (int) (250 + 5 * Math.sin(t * 1.2f)));
        Color front2 = new Color(
                (int) (220 + 15 * Math.sin(t * 0.7f)),
                (int) (215 + 20 * Math.cos(t * 0.9f)),
                (int) (220 + 10 * Math.sin(t * 1.1f)));

        GradientPaint frontGradient = new GradientPaint(x, y, front1, x + width, y + height, front2);
        g2d.setPaint(frontGradient);

        // Rounded corners for lofi style
        g2d.fillRoundRect(x, y, width, height, 20, 20);

        // TAO BIN header with lofi colors
        Color headerColor = lofiColors[(int) (time * 0.01f) % lofiColors.length];
        GradientPaint headerGradient = new GradientPaint(
                x + 15, y + 15, headerColor,
                x + width - 15, y + 55, headerColor.darker());
        g2d.setPaint(headerGradient);
        g2d.fillRoundRect(x + 15, y + 15, width - 30, 50, 15, 15);

        // Add glow effect to header
        g2d.setColor(new Color(headerColor.getRed(), headerColor.getGreen(), headerColor.getBlue(), 100));
        g2d.fillRoundRect(x + 10, y + 10, width - 20, 60, 20, 20);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("TAO BIN", x + 30, y + 38);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("CUBIC BEVERAGE LOFI", x + 30, y + 52);

        // Main display screen with soft edges
        g2d.setColor(new Color(20, 20, 30));
        g2d.fillRoundRect(x + 20, y + 80, width - 40, height - 180, 15, 15);

        // Screen content with softer background
        g2d.setColor(new Color(30, 30, 40));
        g2d.fillRoundRect(x + 25, y + 85, width - 50, height - 190, 10, 10);

        // Draw lofi beverage grid
        drawLofiCubicBeverageGrid(g2d, x + 30, y + 90, width - 60, height - 200);

        // Payment area with gradient
        Color payment1 = new Color(240, 230, 255);
        Color payment2 = new Color(220, 210, 235);
        GradientPaint paymentGradient = new GradientPaint(
                x + 20, y + height - 90, payment1,
                x + width - 20, y + height - 25, payment2);
        g2d.setPaint(paymentGradient);
        g2d.fillRoundRect(x + 20, y + height - 90, width - 40, 65, 10, 10);

        // Dispensing slot with soft glow (moved down)
        drawLofiDispensingArea(g2d, x, y - 32, width, height);

        // Payment slot
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRoundRect(x + width - 50, y + height - 55, 25, 5, 3, 3);
    }

    private void drawLofiDispensingArea(Graphics2D g2d, int x, int y, int width, int height) {
        int areaX = x + 40;
        int areaY = y + height - 50; // Moved up since machine is lower now
        int areaWidth = width - 80;
        int areaHeight = 50;

        // Soft dispensing area background
        GradientPaint dispensingGradient = new GradientPaint(
                areaX, areaY, new Color(60, 60, 80),
                areaX, areaY + areaHeight, new Color(40, 40, 60));
        g2d.setPaint(dispensingGradient);
        g2d.fillRoundRect(areaX, areaY, areaWidth, areaHeight, 15, 15);

        // Add soft inner glow
        g2d.setColor(new Color(100, 150, 200, 80));
        g2d.fillRoundRect(areaX + 5, areaY + 5, areaWidth - 10, areaHeight - 10, 10, 10);

        // Border with lofi color
        g2d.setColor(lofiColors[3]);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(areaX, areaY, areaWidth, areaHeight, 15, 15);
    }

    private void drawLofiCubeRightFace(Graphics2D g2d, int x, int y, int width, int height) {
        // Right face with lofi colors
        int depth = 35;
        Polygon rightFace = new Polygon();
        rightFace.addPoint(x + width, y);
        rightFace.addPoint(x + width + depth, y - depth);
        rightFace.addPoint(x + width + depth, y + height - depth);
        rightFace.addPoint(x + width, y + height);

        Color side1 = lofiColors[1];
        Color side2 = lofiColors[1].darker();
        GradientPaint sideGradient = new GradientPaint(
                x + width, y, side1,
                x + width + depth, y + height - depth, side2);
        g2d.setPaint(sideGradient);
        g2d.fill(rightFace);

        // Add some soft technical details
        g2d.setColor(new Color(side2.getRed(), side2.getGreen(), side2.getBlue(), 150));
        for (int i = 0; i < 10; i++) {
            int lineY = y + 40 + i * 30;
            if (lineY < y + height - 40) {
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(x + width + 5, lineY - i * 3, x + width + depth - 5, lineY - i * 3 - depth);
            }
        }
    }

    private void drawLofiCubeTopFace(Graphics2D g2d, int x, int y, int width, int height) {
        x += 1.5;
        y += 1.5;
        // Top face with dreamy colors
        int depth = 35;
        Polygon topFace = new Polygon();
        topFace.addPoint(x, y);
        topFace.addPoint(x + depth, y - depth);
        topFace.addPoint(x + width + depth, y - depth);
        topFace.addPoint(x + width, y);

        Color top1 = lofiColors[4];
        Color top2 = lofiColors[4].darker();
        GradientPaint topGradient = new GradientPaint(
                x, y, top1,
                x + width + depth, y - depth, top2);
        g2d.setPaint(topGradient);
        g2d.fill(topFace);

        // Add soft ventilation grilles
        g2d.setColor(new Color(top2.getRed(), top2.getGreen(), top2.getBlue(), 180));
        for (int i = 0; i < 5; i++) {
            int grillX = x + 30 + i * 30;
            g2d.fillRoundRect(grillX, y - 15, 20, 4, 2, 2);
            g2d.fillRoundRect(grillX + 10, y - 25, 20, 4, 2, 2);
        }
    }

    private void drawLofiCubeEdges(Graphics2D g2d, int x, int y, int width, int height) {
        int depth = 35;
        g2d.setColor(new Color(lofiColors[0].getRed(), lofiColors[0].getGreen(), lofiColors[0].getBlue(), 200));
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Soft rounded edges
        g2d.drawRoundRect(x, y, width, height, 20, 20);

        // Depth edges with soft lines
        g2d.drawLine(x, y, x + depth, y - depth);
        g2d.drawLine(x + width, y, x + width + depth, y - depth);
        g2d.drawLine(x + width, y + height, x + width + depth, y + height - depth);

        // Top face edges
        g2d.drawLine(x + depth, y - depth, x + width + depth, y - depth);
        g2d.drawLine(x + width + depth, y - depth, x + width + depth, y + height - depth);
    }

    private void drawLofiCubicBeverageGrid(Graphics2D g2d, int x, int y, int width, int height) {
        int rows = 6;
        int cols = 3;
        int cellWidth = width / cols;
        int cellHeight = height / rows;

        // Draw grid of lofi cubic beverage containers
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int cellX = x + col * cellWidth;
                int cellY = y + row * cellHeight;

                // Draw lofi small cube for each beverage
                drawLofiSmallCube(g2d, cellX + 3, cellY + 3, cellWidth - 6, cellHeight - 6, row, col);
            }
        }
    }

    private void drawLofiSmallCube(Graphics2D g2d, int x, int y, int width, int height, int row, int col) {
        // Use lofi color palette with animation
        int colorIndex = (row * 3 + col + (int) (time * 0.01f)) % lofiColors.length;
        Color baseColor = lofiColors[colorIndex];

        // Add breathing effect
        float breathe = 1.0f + 0.05f * (float) Math.sin(time * 0.02f + row * 0.5f + col * 0.3f);
        int adjustedWidth = (int) (width * breathe);
        int adjustedHeight = (int) (height * breathe);
        int adjustedX = x + (width - adjustedWidth) / 2;
        int adjustedY = y + (height - adjustedHeight) / 2;

        // Front face of small cube with gradient
        GradientPaint cubeGradient = new GradientPaint(
                adjustedX, adjustedY, baseColor,
                adjustedX + adjustedWidth, adjustedY + adjustedHeight, baseColor.darker());
        g2d.setPaint(cubeGradient);
        g2d.fillRoundRect(adjustedX, adjustedY, adjustedWidth - 10, adjustedHeight - 10, 5, 5);

        // Right face with lofi shading
        g2d.setColor(new Color(
                Math.max(0, baseColor.getRed() - 40),
                Math.max(0, baseColor.getGreen() - 40),
                Math.max(0, baseColor.getBlue() - 40)));
        Polygon rightFace = new Polygon();
        rightFace.addPoint(adjustedX + adjustedWidth - 10, adjustedY);
        rightFace.addPoint(adjustedX + adjustedWidth - 2, adjustedY - 8);
        rightFace.addPoint(adjustedX + adjustedWidth - 2, adjustedY + adjustedHeight - 18);
        rightFace.addPoint(adjustedX + adjustedWidth - 10, adjustedY + adjustedHeight - 10);
        g2d.fill(rightFace);

        // Top face with highlight
        g2d.setColor(new Color(
                Math.min(255, baseColor.getRed() + 30),
                Math.min(255, baseColor.getGreen() + 30),
                Math.min(255, baseColor.getBlue() + 30)));
        Polygon topFace = new Polygon();
        topFace.addPoint(adjustedX, adjustedY);
        topFace.addPoint(adjustedX + 8, adjustedY - 8);
        topFace.addPoint(adjustedX + adjustedWidth - 2, adjustedY - 8);
        topFace.addPoint(adjustedX + adjustedWidth - 10, adjustedY);
        g2d.fill(topFace);

        // Add soft glow effect
        g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 100));
        g2d.fillRoundRect(adjustedX - 2, adjustedY - 2, adjustedWidth - 6, adjustedHeight - 6, 8, 8);

        // Soft edges
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawRoundRect(adjustedX, adjustedY, adjustedWidth - 10, adjustedHeight - 10, 5, 5);
    }

    private void drawAnimatedVerticalJapaneseText(Graphics2D g2d, String text, int x, int y, int lineSpacing) {
        // Animated color for the text
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
                // Add floating animation to each character
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

                // Draw falling cube
                int halfSize = (int) (item.size / 2);
                g2d.fillRoundRect(-halfSize, -halfSize, (int) item.size, (int) item.size, 5, 5);

                // Add glow effect
                g2d.setColor(new Color(
                        item.color.getRed(),
                        item.color.getGreen(),
                        item.color.getBlue(),
                        (int) (100 * item.alpha)));
                g2d.fillRoundRect(-halfSize - 2, -halfSize - 2, (int) item.size + 4, (int) item.size + 4, 6, 6);

                g2d.setTransform(oldTransform);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        time++;

        // Update weather system - เร็วขึ้น!
        weatherChangeTimer++;
        if (weatherChangeTimer >= weatherDuration) {
            String oldWeather = currentWeather;
            // Make sure we get a different weather
            do {
                currentWeather = weatherEvents[random.nextInt(weatherEvents.length)];
            } while (currentWeather.equals(oldWeather) && weatherEvents.length > 1);

            weatherChangeTimer = 0;
            weatherDuration = 120 + random.nextFloat() * 180; // 2-5 seconds instead of 5-10

            // Add weather change effect particles
            for (int i = 0; i < 15; i++) {
                Particle weatherEffect = new Particle(
                        random.nextFloat() * getWidth(),
                        random.nextFloat() * 200);
                weatherEffect.vx = (float) (Math.random() - 0.5) * 6;
                weatherEffect.vy = (float) (Math.random() - 0.5) * 4;
                weatherEffect.color = lofiColors[random.nextInt(lofiColors.length)];
                weatherEffect.size = random.nextFloat() * 4 + 2;
                particles.add(weatherEffect);
            }
        }

        // Handle machine dropping animation
        if (machineDropped && !machineOnGround) {
            machineVelocityY += 0.8f; // Gravity
            machineY += machineVelocityY;

            // Check if machine hits the ground
            if (machineY >= machineTargetY) {
                machineY = machineTargetY;
                machineVelocityY *= -0.3f; // Small bounce

                if (Math.abs(machineVelocityY) < 2.0f) {
                    machineOnGround = true;
                    machineVelocityY = 0;

                    // Start dispensing timer when machine lands
                    dispensingTimer = new Timer(300 + random.nextInt(500), new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            dispenseRandomItem();
                            // Reset timer with new random interval
                            dispensingTimer.setDelay(300 + random.nextInt(500));
                        }
                    });
                    dispensingTimer.start();

                    // Add landing particles
                    for (int i = 0; i < 30; i++) {
                        int machineX = (getWidth() - 200) / 2;
                        Particle landingEffect = new Particle(
                                machineX + 100 + (random.nextFloat() - 0.5f) * 200,
                                (float) machineY + 420);
                        landingEffect.vx = (float) (Math.random() - 0.5) * 8;
                        landingEffect.vy = (float) (Math.random() * -5 - 2);
                        landingEffect.color = lofiColors[random.nextInt(lofiColors.length)];
                        landingEffect.size = random.nextFloat() * 5 + 2;
                        particles.add(landingEffect);
                    }
                }
            }
        }

        // Update falling items only if machine is on ground
        if (machineOnGround) {
            for (FallingItem item : fallingItems) {
                item.update(floorY, getWidth());
            }

            // Handle collisions between items
            for (int i = 0; i < fallingItems.size(); i++) {
                for (int j = i + 1; j < fallingItems.size(); j++) {
                    FallingItem item1 = fallingItems.get(i);
                    FallingItem item2 = fallingItems.get(j);
                    if (item1.collidesWith(item2)) {
                        item1.handleCollision(item2);
                    }
                }
            }
        }

        // Update particles
        particles.removeIf(particle -> !particle.isAlive());
        for (Particle particle : particles) {
            particle.update();
        }

        // Add new particles randomly
        if (random.nextFloat() < 0.1f) {
            particles.add(new Particle(random.nextFloat() * getWidth(), random.nextFloat() * getHeight()));
        }

        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Manual dispensing when machine is on ground
        if (machineOnGround) {
            dispenseRandomItem();
        }
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

            // Add a subtle window title animation
            Timer titleTimer = new Timer(2000, new ActionListener() {
                private String[] titles = {
                        "立方体の自動販売機 - Lofi Cubic Vending Machine",
                        "TAO BIN - Dreamy Beverage Dispenser ✨",
                        "Lofi Vending Experience 🌸",
                        "Cubic Dreams & Refreshments 🎵"
                };
                private int titleIndex = 0;

                @Override
                public void actionPerformed(ActionEvent e) {
                    titleIndex = (titleIndex + 1) % titles.length;
                    frame.setTitle(titles[titleIndex]);
                }
            });
            titleTimer.start();
        });
    }
}