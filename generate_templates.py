import os
import json
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from skimage.morphology import skeletonize

def order_points_greedy(points):
    if len(points) <= 1:
        return points
    
    # Start with the point that has the minimum X (leftmost)
    # For a tie, pick the one with minimum Y
    start_idx = np.lexsort((points[:, 1], points[:, 0]))[0]
    
    ordered = [points[start_idx]]
    remaining = list(points)
    remaining.pop(start_idx)
    
    while remaining:
        last_pt = ordered[-1]
        dist = np.sqrt(np.sum((np.array(remaining) - last_pt)**2, axis=1))
        nearest_idx = np.argmin(dist)
        ordered.append(remaining[nearest_idx])
        remaining.pop(nearest_idx)
        
    return np.array(ordered)

def generate_template(char, font_path, size=280):
    try:
        font = ImageFont.truetype(font_path, size)
    except IOError:
        print(f"Error: Cannot load font at {font_path}")
        return []

    # Create a blank image with black background
    img = Image.new('L', (size * 2, size * 2), 0)
    draw = ImageDraw.Draw(img)
    
    # Draw text in white
    # Use bounding box to center it
    bbox = draw.textbbox((0, 0), char, font=font)
    w = bbox[2] - bbox[0]
    h = bbox[3] - bbox[1]
    
    draw.text((size - w/2, size - h/2), char, font=font, fill=255)
    
    # Convert to numpy array
    arr = np.array(img)
    
    # Binarize
    binary = arr > 128
    
    if not np.any(binary):
        return []
    
    # Skeletonize
    skeleton = skeletonize(binary)
    
    # Extract points (y, x) -> (x, y)
    y, x = np.nonzero(skeleton)
    points = np.column_stack((x, y))
    
    # Order points
    ordered_points = order_points_greedy(points)
    
    # Format to TS Stroke array format
    stroke = [{"x": float(pt[0]), "y": float(pt[1])} for pt in ordered_points]
    
    return [stroke] # Return as array of strokes (only 1 stroke since we combined them)

def main():
    font_path = "frontend/public/fonts/HP001.ttf"
    if not os.path.exists(font_path):
        print(f"Font not found: {font_path}")
        return
        
    chars = [
        'a', 'ă', 'â', 'b', 'c', 'd', 'đ', 'e', 'ê', 'g', 'h', 'i', 'k', 'l', 'm', 'n', 'o', 'ô', 'ơ', 'p', 'q', 'r', 's', 't', 'u', 'ư', 'v', 'x', 'y',
        'A', 'Ă', 'Â', 'B', 'C', 'D', 'Đ', 'E', 'Ê', 'G', 'H', 'I', 'K', 'L', 'M', 'N', 'O', 'Ô', 'Ơ', 'P', 'Q', 'R', 'S', 'T', 'U', 'Ư', 'V', 'X', 'Y'
    ]
    templates = {}
    for char in chars:
        print(f"Generating template for '{char}'...")
        strokes = generate_template(char, font_path)
        if strokes and len(strokes[0]) > 0:
            templates[char] = strokes
            
    # Write to templateData.ts
    out_file = "frontend/src/pages/Student/writing/engine/templateData.ts"
    
    ts_code = f"""import type {{ Stroke }} from './types';

// GENERATED FILE - Do not edit manually.
// Templates generated offline using Python skeletonization.

export const TEMPLATE_STROKES: Record<string, Stroke[]> = {json.dumps(templates, indent=2)};

export function getTemplateForLetter(letter: string): Stroke[] | null {{
  return TEMPLATE_STROKES[letter.toLowerCase()] || null;
}}
"""
    with open(out_file, "w", encoding='utf-8') as f:
        f.write(ts_code)
        
    print(f"Successfully generated {len(templates)} templates and saved to {out_file}")

if __name__ == "__main__":
    main()
